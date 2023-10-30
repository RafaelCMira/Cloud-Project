package scc.srv.rentals;

import com.azure.cosmos.CosmosException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.users.UsersResource;
import scc.srv.houses.HousesService;
import scc.srv.users.UsersService;
import scc.srv.utils.Cache;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static scc.srv.utils.Utility.*;

public class RentalResource implements RentalService {
    public static final String CONTAINER = "rentals";
    public static final String PARTITION_KEY = "/houseId";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createRental(String houseId, RentalDAO rentalDAO) throws Exception {
        //TODO: add rental to house with azure functions
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            checkRentalCreation(houseId, rentalDAO, jedis);

            var newRental = db.createItem(rentalDAO, CONTAINER);
            int statusCode = newRental.getStatusCode();

            if (isStatusOk(statusCode)) {

                // Check if the right rental it is in the DB
                var rentalInDB = db.getRentalById(houseId, rentalDAO.getId()).stream().findFirst();
                if (rentalInDB.isEmpty())
                    return sendResponse(INTERNAL_SERVER_ERROR);
                else if (!rentalInDB.get().getUserId().equals(rentalDAO.getUserId()))
                    return sendResponse(CONFLICT, RENTAL_MSG, rentalDAO.getId());

                Cache.putInCache(rentalDAO, CACHE_PREFIX, jedis);
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
        else
            return processException(statusCode, USER_MSG, rental.getUserId());
    }

    @Override
    public Response getRental(String houseID, String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        // Check if it is in Cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String rCache = jedis.get(CACHE_PREFIX + id);
            if (rCache != null)
                return sendResponse(OK, mapper.readValue(rCache, RentalDAO.class).toRental());

            CosmosPagedIterable<RentalDAO> res = db.getRentalById(houseID, id);
            Optional<RentalDAO> result = res.stream().findFirst();
            if (result.isPresent()) {
                Cache.putInCache(result.get(), CACHE_PREFIX, jedis);
                return sendResponse(OK, result.get().toRental());
            } else {
                return sendResponse(NOT_FOUND, RENTAL_MSG, id);
            }
        }
    }

    @Override
    public Response updateRental(String houseID, String id, RentalDAO rentalDAO) throws Exception {
        RentalDAO updatedRental = refactorRental(houseID, id, rentalDAO);
        try {
            var res = db.updateRental(updatedRental);
            int statusCode = res.getStatusCode();
            if (isStatusOk(res.getStatusCode())) {
                try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                    jedis.set(CACHE_PREFIX + id, mapper.writeValueAsString(updatedRental));
                }
                return sendResponse(OK, updatedRental.toRental());
            } else {
                return processException(statusCode, RENTAL_MSG, id);
            }
        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), RENTAL_MSG, id);
        }

    }

    @Override
    public Response listRentals(String houseID) {
        //TODO: chache ?
        var res = db.getItems(CONTAINER, RentalDAO.class).stream().map(RentalDAO::toRental).toList();
        return sendResponse(OK, res);
    }

    @Override
    public Response deleteRental(String houseId, String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        var res = db.deleteRental(houseId,id);
        int statusCode = res.getStatusCode();

        if (isStatusOk(statusCode)) {
            // TODO - This could be made with azure functions
            var houseDAO = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
            if(houseDAO.isEmpty()) return sendResponse(NOT_FOUND,HOUSE_MSG,houseId);
            houseDAO.get().removeRental(id);

            // Delete rental in cache
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                jedis.del(CACHE_PREFIX + id);
            }
            String s = String.format("StatusCode: %d \nRental %s was delete", statusCode, id);
            return sendResponse(OK, s);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }


    private RentalDAO refactorRental(String houseID, String id, RentalDAO rentalDAO) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (Null ID)");

        var result = db.getRentalById(houseID, id).stream().findFirst();
        if (result.isPresent()) {
            RentalDAO rental = result.get();

            String rentalDAOId = rentalDAO.getId();
            if (!rental.getId().equals(rentalDAOId))
                rental.setId(rentalDAOId);

            String rentalDAOHouseId = rentalDAO.getHouseId();
            if (!rental.getHouseId().equals(rentalDAOHouseId))
                rental.setHouseId(rentalDAOHouseId);

            String rentalDAOUserId = rentalDAO.getUserId();
            if (!rental.getUserId().equals(rentalDAOUserId))
                rental.setUserId(rentalDAOUserId);

            double rentalDAOPrice = rentalDAO.getPrice();
            if (rental.getPrice() != (rentalDAOPrice))
                rental.setPrice(rentalDAOPrice);

            Date rentalDAOInitialDate = rentalDAO.getInitialDate();
            if (!rental.getInitialDate().equals(rentalDAOInitialDate))
                rental.setInitialDate(rentalDAOInitialDate);

            Date rentalDAOEndDate = rentalDAO.getEndDate();
            if (!rental.getEndDate().equals(rentalDAOEndDate))
                rental.setEndDate(rentalDAOEndDate);

            return rental;

        } else {
            throw new Exception("Error: 404 Rental Not Found");
        }
    }

    @Override
    public Response getDiscountedRentals(String houseID) throws Exception {
        //TODO: Not working, conflict on the endpoint
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
     * @throws Exception - WebApplicationException depending of the result of the checks
     */
    private void checkRentalCreation(String houseId, RentalDAO rental, Jedis jedis) throws Exception {
        if (badParams(rental.getId(), rental.getHouseId(), rental.getUserId()))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        // Verify if house exists
        HouseDAO house = mapper.readValue(jedis.get(HousesService.HOUSE_PREFIX + houseId), HouseDAO.class);
        if (house == null) {
            var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
            if (houseRes.isPresent())
                house = houseRes.get();
            else
                throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);
        }

        // Verify if user exists
        String userId = rental.getUserId();
        if (jedis.get(UsersService.USER_PREFIX + userId) == null) {
            var userRes = db.getById(userId, UsersResource.CONTAINER, UserDAO.class).stream().findFirst();
            if (userRes.isEmpty())
                throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);
        }

        if(isHouseNotAvailable(houseId,rental.getInitialDate(),rental.getEndDate()))
            throw new WebApplicationException(RENTAL_MSG, Response.Status.CONFLICT);
    }

    /**
     * Checks if the House is not available in the given time.
     * @param houseId - The id of the house.
     * @param sTime - The start Date of the Rental.
     * @param eTime - The end Date of the Rental
     * @return true if the house is not available in the given time, false otherwise.
     */
    private boolean isHouseNotAvailable(String houseId, Date sTime, Date eTime) {
        var rentals = db.getRentals(houseId).stream().toList();

        for(RentalDAO r: rentals) {
           if(!r.getInitialDate().after(eTime)&&!r.getEndDate().before(sTime))
               return true;
        }

        return false;
    }
}
