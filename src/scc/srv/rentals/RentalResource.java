package scc.srv.rentals;

import com.azure.cosmos.CosmosException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static scc.srv.utils.Utility.*;

public class RentalResource implements RentalService {
    public static final String CONTAINER = "rentals";
    public static final String PARTITION_KEY = "/id";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createRental(String houseId, RentalDAO rentalDAO) throws Exception {
            //TODO: adicionar o rental à casa
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            var house = checkRentalCreation(houseId,rentalDAO,jedis);

            var newRental = db.createItem(rentalDAO, CONTAINER);
            int statusCode = newRental.getStatusCode();

            if (isStatusOk(statusCode)) {
                // TODO verificar se o rental foi realmente adicionado
                var rentalInDB = db.getRentalById(houseId,rentalDAO.getId()).stream().findFirst().get();
                if(!rentalInDB.getUserId().equals(rentalDAO.getUserId()))
                    return sendResponse(CONFLICT,"Rental",rentalDAO.getId());

                Cache.putInCache(rentalDAO,CACHE_PREFIX,jedis);
                return sendResponse(OK, rentalDAO.toRental().toString());
            } else
                return processException(statusCode,"Rental",rentalDAO.getId());

        }  catch (CosmosException ex) {
            return processException(ex.getStatusCode(),"Rental",rentalDAO.getId());
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(),ex.getMessage(),rentalDAO);
        }
    }

    private Response handleCreateException(int statusCode, String msg, RentalDAO rental) {
        if (msg.contains("House"))
            return processException(statusCode, "House", rental.getHouseId());
        else
            return processException(statusCode, "User", rental.getUserId());
    }

    @Override
    public Response getRental(String houseID, String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (Null ID)");

        // Check if it is in Cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String rCache = jedis.get(CACHE_PREFIX + id);
            if (rCache != null)
                return sendResponse(OK,mapper.readValue(rCache, RentalDAO.class).toRental());

            CosmosPagedIterable<RentalDAO> res = db.getRentalById(houseID, id);
            Optional<RentalDAO> result = res.stream().findFirst();
            if (result.isPresent()) {
                Cache.putInCache(result.get(),CACHE_PREFIX,jedis);
                return sendResponse(OK,result.get().toRental());
            } else {
                return sendResponse(NOT_FOUND,"Rental",id);
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
                return sendResponse(OK,updatedRental.toRental());
            } else {
                return processException(statusCode,"Rental",id);
            }
        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(),"Rental",id);
        }

    }

    @Override
    public Response listRentals(String houseID) {
        //TODO: chache ?
        var res = db.getItems(CONTAINER, RentalDAO.class).stream().map(RentalDAO::toRental).toList();
        return sendResponse(OK,res);
    }

    @Override
    public Response deleteRental(String houseId, String id) throws Exception {
        if (id == null)
            throw new Exception("Error: 400 Bad Request (Null ID");

        var res = db.deleteRental(id);
        int statusCode = res.getStatusCode();

        if (isStatusOk(statusCode)) {
            HouseDAO houseDAO = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst().get();
            houseDAO.removeRental(id);
            //TODO: delete do rental na casa chache
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                jedis.del(CACHE_PREFIX + id);
            }
            String s = String.format("StatusCode: %d \nRental %s was delete", statusCode, id);
            return sendResponse(OK,s);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }


    private RentalDAO refactorRental(String houseID, String id, RentalDAO rentalDAO) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (Null ID)");

        CosmosPagedIterable<RentalDAO> res = db.getRentalById(houseID, id);
        Optional<RentalDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            RentalDAO r = result.get();

            String rentalDAOId = rentalDAO.getId();
            if (!r.getId().equals(rentalDAOId))
                r.setId(rentalDAOId);

            String rentalDAOHouseId = rentalDAO.getHouseId();
            if (!r.getHouseId().equals(rentalDAOHouseId))
                r.setHouseId(rentalDAOHouseId);

            String rentalDAOUserId = rentalDAO.getUserId();
            if (!r.getUserId().equals(rentalDAOUserId))
                r.setUserId(rentalDAOUserId);

            double rentalDAOPrice = rentalDAO.getPrice();
            if (r.getPrice() != (rentalDAOPrice))
                r.setPrice(rentalDAOPrice);

            Date rentalDAOInitialDate = rentalDAO.getInitialDate();
            if (!r.getInitialDate().equals(rentalDAOInitialDate))
                r.setInitialDate(rentalDAOInitialDate);

            Date rentalDAOEndDate = rentalDAO.getEndDate();
            if (!r.getEndDate().equals(rentalDAOEndDate))
                r.setEndDate(rentalDAOEndDate);

            return r;

        } else {
            throw new Exception("Error: 404 Rental Not Found");
        }
    }

    @Override
    public Response getDiscountedRentals(String houseID) throws Exception {
        //TODO: Not working, conflict on the endpoint
        //TODO: chache & tem ser updated de x em x tempo
        var rentalsDAO = db.getRentals(houseID);
        List<Rental> res = new ArrayList<>();
        for (RentalDAO r : rentalsDAO) {
            if (r.getDiscount() > 0 && !r.getInitialDate().before(Date.from(Instant.now())))
                res.add(r.toRental());
        }
        return sendResponse(OK,res);
    }

    /**
     * Checks if the Rental can be created
     * @param houseId - id of the house
     * @param rental - the rental
     * @return afdfadfa
     * @throws Exception - WebApplicationException depending of the result of the checks
     */
    private HouseDAO checkRentalCreation(String houseId, RentalDAO rental,Jedis jedis) throws Exception{
        if (badParams(rental.getId(), rental.getHouseId(), rental.getUserId()))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        // Verify if house exists
        HouseDAO house = mapper.readValue(jedis.get(HousesService.HOUSE_PREFIX + houseId), HouseDAO.class);
        if (house == null) {
            var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
            if (houseRes.isPresent())
                house = houseRes.get();
            else
                throw new WebApplicationException("House",Response.Status.NOT_FOUND);
        }

        // Verify if user exists
        String userId = rental.getUserId();
        if (jedis.get(UsersService.USER_PREFIX + userId) == null) {
            var userRes = db.getById(userId, UsersResource.CONTAINER, UserDAO.class).stream().findFirst();
            if (userRes.isEmpty())
                throw new WebApplicationException("User", Response.Status.NOT_FOUND);
        }
        // TODO - check if the house is available
        // checkHouseAvailability(house);

        return house;
    }
}
