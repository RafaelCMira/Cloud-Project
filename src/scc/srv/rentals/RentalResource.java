package scc.srv.rentals;

import com.azure.cosmos.CosmosException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesService;
import scc.srv.utils.Validations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static scc.srv.utils.Utility.*;

public class RentalResource extends Validations implements RentalService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public Response createRental(Cookie session, String houseId, RentalDAO rentalDAO) throws Exception {
        //TODO: add rental to house with azure functions
        try {
            checkRentalCreation(session, houseId, rentalDAO);

            db.create(rentalDAO, CONTAINER);

            // check se realmente foi o meu rental a ir para a DB
            var rentalInDB = Validations.rentalExists(rentalDAO.getId());

            if (rentalInDB == null)
                return sendResponse(INTERNAL_SERVER_ERROR);

            if (!rentalInDB.getId().equals(rentalDAO.getId()) || !rentalInDB.getUserId().equals(rentalDAO.getUserId()))
                return sendResponse(CONFLICT, RENTAL_MSG, rentalDAO.getId());

            Cache.putInCache(rentalDAO, RENTAL_PREFIX);

            return sendResponse(OK, rentalDAO.toRental());

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
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var rental = Validations.rentalExists(id);
            if (rental == null)
                return sendResponse(NOT_FOUND, RENTAL_MSG, id);

            Cache.putInCache(rental, RENTAL_PREFIX);

            return sendResponse(OK, rental.toRental());

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    @Override
    public Response updateRental(Cookie session, String houseId, String id, RentalDAO rentalDAO) throws Exception {

        try {
            var updatedRental = genUpdatedRental(session, houseId, id, rentalDAO);

            db.update(updatedRental, RentalService.CONTAINER, updatedRental.getHouseId());

            Cache.putInCache(updatedRental, RENTAL_PREFIX);

            return sendResponse(OK, updatedRental.toRental());

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), RENTAL_MSG, id);
        }

    }

    @Override
    public Response listRentals(String houseID) {
        //TODO: cache ?
        var res = db.getAll(CONTAINER, RentalDAO.class).stream().map(RentalDAO::toRental).toList();
        return sendResponse(OK, res);
    }

    @Override
    public Response deleteRental(String houseId, String id) throws Exception {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var house = Validations.houseExists(id);
            if (house == null)
                return sendResponse(NOT_FOUND, HOUSE_MSG, id);

            db.delete(id, CONTAINER, PARTITION_KEY);

            Cache.deleteFromCache(RENTAL_PREFIX, id);

            //TODO: é mesmo necessário guardar os Ids dos rentals??
            // acho que não porque temos os rentals guardados por houseId como partitionKey logo quando fizermos uma query seria eficiente
            // Eliminavam-se estas 3 linhas abaixo
            house.removeRental(id);
            db.update(house, HousesService.CONTAINER, house.getId());

            Cache.putInCache(house, HousesService.HOUSE_PREFIX);

            return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, RENTAL_MSG, id));

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage(), id);
        }
    }


    private RentalDAO genUpdatedRental(Cookie session, String houseId, String id, RentalDAO rental) throws Exception {
        if (Validations.badParams(id))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        var house = Validations.houseExists(houseId);
        if (house == null)
            throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);

        var rentalDAO = Validations.rentalExists(id);
        if (rentalDAO == null)
            throw new WebApplicationException(RENTAL_MSG, Response.Status.NOT_FOUND);

        //todo: Apenas o dono da casa pode fazer update
        var checkCookies = checkUserSession(session, house.getOwnerId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

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
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        if (Validations.badParams(rental.getId(), rental.getHouseId(), rental.getUserId()))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        if (!houseId.equals(rental.getHouseId()))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        if (Validations.houseExists(houseId) == null)
            throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);

        if (Validations.userExists(rental.getUserId()) == null)
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);

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
