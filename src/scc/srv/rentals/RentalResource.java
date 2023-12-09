package scc.srv.rentals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.MongoDBLayer;
import scc.srv.houses.HousesService;
import scc.srv.users.UsersService;
import scc.srv.utils.Validations;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static scc.srv.utils.Utility.*;

public class RentalResource extends Validations implements RentalService {

    private final MongoDBLayer db = MongoDBLayer.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createRental(Cookie session, String houseId, RentalDAO rentalDAO) throws Exception {
        try {
            rentalDAO.setId(UUID.randomUUID().toString());
            rentalDAO.setHouseId(houseId);

            var house = checkRentalCreation(session, houseId, rentalDAO);

            long daysBetween = ChronoUnit.DAYS.between(
                    rentalDAO.getInitialDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    rentalDAO.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            );

            rentalDAO.setPrice((int) (daysBetween * (house.getPrice() - house.getDiscount())));

            db.create(rentalDAO, COLLECTION);

            // Check if the rental was successfull or some concurrent one got there first
            var rentalInDB = Validations.rentalExists(rentalDAO.getId());

            if (rentalInDB == null)
                return sendResponse(INTERNAL_SERVER_ERROR);

            if (!rentalInDB.getId().equals(rentalDAO.getId()) || !rentalInDB.getUserId().equals(rentalDAO.getUserId()))
                return sendResponse(CONFLICT, RENTAL_MSG, rentalDAO.getId());

            Cache.putInCache(rentalDAO, RENTAL_PREFIX);

            house.setRentalsCounter(house.getRentalsCounter() + 1);
            db.update(house.getId(), house, HousesService.COLLECTION);

            return sendResponse(OK, rentalDAO.toRental());

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), rentalDAO);
        }
    }

    @Override
    public Response getRental(String houseId, String id) throws Exception {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var rental = Validations.rentalExists(id);
            if (rental == null)
                return sendResponse(NOT_FOUND, RENTAL_MSG, id);

            var rentalHouse = rental.getHouseId();
            if (!houseId.equals(rentalHouse)) {
                return sendResponse(BAD_REQUEST, String.format(RENTAL_NOT_BELONG_TO_HOUSE, id, houseId));
            }

            Cache.putInCache(rental, RENTAL_PREFIX);

            return sendResponse(OK, rental.toRental());

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        }
    }

    @Override
    public Response deleteRental(String houseId, String id) {
        try {
            var checks = checkRentalDeletion(houseId, id);
            if (checks.getStatus() != Response.Status.OK.getStatusCode())
                return checks;

            db.delete(id, COLLECTION);

            Cache.deleteFromCache(RENTAL_PREFIX, id);

            return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, RENTAL_MSG, id));

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        }
    }

    @Override
    public Response updateRental(Cookie session, String houseId, String id, RentalDAO rentalDAO) throws Exception {
        try {
            var updatedRental = genUpdatedRental(session, houseId, id, rentalDAO);
            rentalDAO = updatedRental;

            db.update(updatedRental.getId(), updatedRental, RentalService.COLLECTION);

            Cache.putInCache(updatedRental, RENTAL_PREFIX);

            return sendResponse(OK, updatedRental.toRental());

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), rentalDAO);
        }
    }

    @Override
    public Response listHouseRentals(String houseId, int offset) {
        try {
            if (Validations.houseExists(houseId) == null)
                return sendResponse(NOT_FOUND, HOUSE_MSG, HOUSE_ID);

            List<Rental> houseRentals = new ArrayList<>();

            String key = String.format(HOUSE_RENTALS, houseId, offset);
            var cacheHouses = Cache.getListFromCache(key);
            if (!cacheHouses.isEmpty()) {
                for (var house : cacheHouses) {
                    houseRentals.add(mapper.readValue(house, Rental.class));
                }
                return sendResponse(OK, houseRentals);
            }

            houseRentals = db.listHouseRentals(houseId, offset);

            Cache.putListInCache(houseRentals, key);

            return sendResponse(OK, houseRentals);

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (JsonProcessingException e) {
            return processException(500);
        }
    }


    private RentalDAO genUpdatedRental(Cookie session, String houseId, String id, RentalDAO rental) throws Exception {
        if (Validations.badParams(id))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        var house = Validations.houseExists(houseId);
        if (house == null)
            throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);

        var checkCookies = checkUserSession(session, house.getOwnerId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        var rentalDAO = Validations.rentalExists(id);
        if (rentalDAO == null)
            throw new WebApplicationException(RENTAL_MSG, Response.Status.NOT_FOUND);

        var rentalHouse = rentalDAO.getHouseId();
        if (!houseId.equals(rentalHouse)) {
            throw new WebApplicationException(String.format(RENTAL_NOT_BELONG_TO_HOUSE, id, houseId), Response.Status.BAD_REQUEST);
        }

        var newPrice = rental.getPrice();
        if (newPrice != null)
            if (newPrice > 0)
                rentalDAO.setPrice(newPrice);
            else
                throw new WebApplicationException(INVALID_PRICE, Response.Status.BAD_REQUEST);

        return rentalDAO;
    }

    private HouseDAO checkRentalCreation(Cookie session, String houseId, RentalDAO rental) throws Exception {
        if (Validations.userExists(rental.getUserId()) == null)
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);

        var checkCookies = checkUserSession(session, rental.getUserId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        if (Validations.badParams(rental.getUserId()) || Validations.datesNotValid(rental.getInitialDate(), rental.getEndDate()))
            throw new WebApplicationException("Something in your request is wrong. Check dates pls.", Response.Status.BAD_REQUEST);

        var house = Validations.houseExists(houseId);
        if (house == null)
            throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);
        else if (house.getOwnerId().equals(UsersService.DELETED_USER))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        if (!Validations.isAvailable(houseId, rental.getInitialDate(), rental.getEndDate()))
            throw new WebApplicationException(RENTAL_MSG, Response.Status.CONFLICT);

        return house;
    }

    private Response checkRentalDeletion(String houseId, String id) {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        var house = Validations.houseExists(houseId);
        if (house == null)
            return sendResponse(NOT_FOUND, HOUSE_MSG, houseId);

        // Check if rental belongs to the house
        var rental = Validations.rentalExists(id);
        if (rental == null) {
            return sendResponse(NOT_FOUND, RENTAL_MSG, id);
        } else {
            var rentalHouse = rental.getHouseId();
            if (!houseId.equals(rentalHouse)) {
                return sendResponse(BAD_REQUEST, String.format(RENTAL_NOT_BELONG_TO_HOUSE, id, houseId));
            }
        }

        return Response.ok().build();
    }

    private Response handleCreateException(int statusCode, String msg, RentalDAO rental) {
        if (statusCode == 409)
            return Response.status(Response.Status.CONFLICT)
                    .entity(String.format("House %s already rented for this period", rental.getHouseId()))
                    .build();
        if (statusCode == 403)
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Can't rent this house. Owner is no longer in the system.")
                    .build();
        if (msg.contains(HOUSE_MSG))
            return processException(statusCode, HOUSE_MSG, rental.getHouseId());
        else if (msg.contains(USER_MSG))
            return processException(statusCode, USER_MSG, rental.getUserId());
        else if (msg.contains(RENTAL_MSG))
            return processException(statusCode, RENTAL_MSG, rental.getId());
        else
            return processException(statusCode, msg);
    }

}
