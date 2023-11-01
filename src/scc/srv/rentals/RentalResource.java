package scc.srv.rentals;

import com.azure.cosmos.CosmosException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import scc.cache.Cache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.houses.HousesService;
import scc.srv.users.UsersService;
import scc.utils.mgt.AzureManagement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static scc.srv.utils.Utility.*;

public class RentalResource implements RentalService {
    public static final String CONTAINER = "rentals";
    public static final String PARTITION_KEY = "/houseId";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createRental(Cookie session, String houseId, RentalDAO rentalDAO) throws Exception {
        //TODO: add rental to house with azure functions
        try {
            checkRentalCreation(session, houseId, rentalDAO);

            var newRental = db.createItem(rentalDAO, CONTAINER);
            int statusCode = newRental.getStatusCode();

            if (isStatusOk(statusCode)) {

                // Check if the right rental it is in the DB
                var rentalInDB = db.getRentalById(houseId, rentalDAO.getId()).stream().findFirst();
                if (rentalInDB.isEmpty())
                    return sendResponse(INTERNAL_SERVER_ERROR);
                else if (!rentalInDB.get().getUserId().equals(rentalDAO.getUserId()))
                    return sendResponse(CONFLICT, RENTAL_MSG, rentalDAO.getId());

                if (AzureManagement.CREATE_REDIS) {
                    Cache.putInCache(rentalDAO, RENTAL_PREFIX);
                }

                return sendResponse(OK, rentalDAO.toRental().toString());

            } else
                return processException(statusCode, RENTAL_MSG, rentalDAO.getId());

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), RENTAL_MSG, rentalDAO.getId());
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), rentalDAO);
        }
    }

    private Response handleCreateException(int statusCode, String msg, RentalDAO rental) {
        if (msg.contains(HOUSE_MSG))
            return processException(statusCode, HOUSE_MSG, rental.getHouseId());
        else if (msg.contains(USER_MSG))
            return processException(statusCode, USER_MSG, rental.getUserId());
        else
            return processException(statusCode, RENTAL_MSG, rental.getId());
    }

    @Override
    public Response getRental(String houseId, String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            if (AzureManagement.CREATE_REDIS) {
                String cacheRes = Cache.getFromCache(RENTAL_PREFIX, id);
                if (cacheRes != null)
                    return sendResponse(OK, mapper.readValue(cacheRes, RentalDAO.class).toRental());
            }

            var result = db.getRentalById(houseId, id).stream().findFirst();
            if (result.isPresent()) {
                var rentalToCache = result.get();

                if (AzureManagement.CREATE_REDIS) {
                    Cache.putInCache(rentalToCache, RENTAL_PREFIX);
                }

                return sendResponse(OK, rentalToCache.toRental());

            } else
                return sendResponse(NOT_FOUND, RENTAL_MSG, id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    @Override
    public Response updateRental(Cookie session, String houseId, String id, RentalDAO rentalDAO) throws Exception {
        RentalDAO updatedRental = genUpdatedRental(session, houseId, id, rentalDAO);
        try {
            var res = db.updateRental(updatedRental);
            int statusCode = res.getStatusCode();
            if (isStatusOk(res.getStatusCode())) {
                if (AzureManagement.CREATE_REDIS) {
                    Cache.putInCache(updatedRental, RENTAL_PREFIX);
                }

                return sendResponse(OK, updatedRental.toRental());

            } else
                return processException(statusCode, RENTAL_MSG, id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), RENTAL_MSG, id);
        }

    }

    @Override
    public Response listRentals(String houseID) {
        //TODO: cache ?
        var res = db.getItems(CONTAINER, RentalDAO.class).stream().map(RentalDAO::toRental).toList();
        return sendResponse(OK, res);
    }

    @Override
    public Response deleteRental(String houseId, String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        var res = db.deleteRental(houseId, id);
        int statusCode = res.getStatusCode();

        if (isStatusOk(statusCode)) {
            // TODO - This could be made with azure functions
            var houseDAO = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
            if (houseDAO.isEmpty()) return sendResponse(NOT_FOUND, HOUSE_MSG, houseId);
            houseDAO.get().removeRental(id);

            // Delete rental in cache
            if (AzureManagement.CREATE_REDIS) {
                Cache.deleteFromCache(RENTAL_PREFIX, id);
            }

            String s = String.format("StatusCode: %d \nRental %s was delete", statusCode, id);
            return sendResponse(OK, s);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }


    private RentalDAO genUpdatedRental(Cookie session, String houseID, String id, RentalDAO rental) throws Exception {
        if (badParams(id))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        var result = db.getRentalById(houseID, id).stream().findFirst();
        if (result.isPresent()) {
            RentalDAO rentalDAO = result.get();

            //todo: Quem pode fazer update ao rental? O dono da casa ou o user que aluga?
           /* var checkCookies = checkUserSession(session, rentalDAO.getUserId());
            if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
                throw new WebApplicationException(UNAUTHORIZED_MSG, Response.Status.UNAUTHORIZED);
            */

            //TODO: ver o que se pode alterar num rental. Verficar datas de novo
            double rentalDAOPrice = rental.getPrice();
            if (rentalDAO.getPrice() != (rentalDAOPrice))
                rentalDAO.setPrice(rentalDAOPrice);

            Date rentalDAOInitialDate = rental.getInitialDate();
            if (!rentalDAO.getInitialDate().equals(rentalDAOInitialDate))
                rentalDAO.setInitialDate(rentalDAOInitialDate);

            Date rentalDAOEndDate = rental.getEndDate();
            if (!rentalDAO.getEndDate().equals(rentalDAOEndDate))
                rentalDAO.setEndDate(rentalDAOEndDate);

            return rentalDAO;

        } else {
            throw new WebApplicationException(RENTAL_MSG, Response.Status.NOT_FOUND);
        }
    }

    @Override
    public Response getDiscountedRentals(String houseID) throws Exception {
        //TODO: cache & tem ser updated de x em x tempo
        var rentalsDAO = db.getRentals(houseID);
        List<Rental> res = new ArrayList<>();
        for (RentalDAO r : rentalsDAO) {
            if (r.getDiscount() > 0 && !r.getInitialDate().before(Date.from(Instant.now())))
                res.add(r.toRental());
        }
        return sendResponse(OK, res);
    }

    /**
     * Checks if the Rental can be created
     *
     * @param houseId - id of the house
     * @param rental  - the rental
     * @throws Exception - WebApplicationException depending on the result of the checks
     */
    private void checkRentalCreation(Cookie session, String houseId, RentalDAO rental) throws Exception {
        var checkCookies = checkUserSession(session, rental.getUserId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(UNAUTHORIZED, Response.Status.UNAUTHORIZED);

        if (badParams(rental.getId(), rental.getHouseId(), rental.getUserId()))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        // Verify if house exists
        if (Cache.getFromCache(HousesService.HOUSE_PREFIX, rental.getHouseId()) == null) {
            var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
            if (houseRes.isEmpty())
                throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);
        }

        // Verify if house exists
        if (Cache.getFromCache(UsersService.USER_PREFIX, rental.getUserId()) == null) {
            var userRes = db.getById(rental.getUserId(), HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
            if (userRes.isEmpty())
                throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);
        }

        // Verify if house is available
        if (isHouseNotAvailable(houseId, rental.getInitialDate(), rental.getEndDate()))
            throw new WebApplicationException(RENTAL_MSG, Response.Status.CONFLICT);
    }

    /**
     * Checks if the House is not available in the given time.
     *
     * @param houseId - The id of the house.
     * @param sTime   - The start Date of the Rental.
     * @param eTime   - The end Date of the Rental
     * @return true if the house is not available in the given time, false otherwise.
     */
    private boolean isHouseNotAvailable(String houseId, Date sTime, Date eTime) {
        var rentals = db.getRentals(houseId).stream().toList();

        for (RentalDAO r : rentals) {
            if (!r.getInitialDate().after(eTime) && !r.getEndDate().before(sTime))
                return true;
        }

        return false;
    }
}
