package scc.srv.rentals;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesService;
import scc.srv.users.UsersService;
import scc.srv.utils.Validations;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static scc.srv.utils.Utility.*;

public class RentalResource extends Validations implements RentalService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
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

            db.create(rentalDAO, CONTAINER);

            // check se realmente foi o meu rental a ir para a DB
            var rentalInDB = Validations.rentalExists(rentalDAO.getId());

            if (rentalInDB == null)
                return sendResponse(INTERNAL_SERVER_ERROR);

            if (!rentalInDB.getId().equals(rentalDAO.getId()) || !rentalInDB.getUserId().equals(rentalDAO.getUserId()))
                return sendResponse(CONFLICT, RENTAL_MSG, rentalDAO.getId());

            Cache.putInCache(rentalDAO, RENTAL_PREFIX);
            Cache.addToListInCache(rentalDAO.toRental(), HOUSE_RENTALS);

            return sendResponse(OK, rentalDAO.toRental());

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), RENTAL_MSG, rentalDAO.getId());
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), rentalDAO);
        }
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
    public Response deleteRental(String houseId, String id) {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        // todo: quando fazemos delete de um rental que não é desta casa vai dar erro que não tratamos ainda
        try {
            var house = Validations.houseExists(houseId);
            if (house == null)
                return sendResponse(NOT_FOUND, HOUSE_MSG, houseId);

            db.delete(id, CONTAINER, houseId);

            Cache.deleteFromCache(RENTAL_PREFIX, id);
            Cache.removeListIncCache(HOUSE_RENTALS);

            return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, RENTAL_MSG, id));

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage(), id);
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
    public Response listHouseRentals(String houseId) {
        //TODO: rafactor this method
        try {
            var houseRentals = Cache.getListFromCache(HOUSE_RENTALS);

            List<Rental> toReturn = new ArrayList<>();
            for (String rental : houseRentals) {
                toReturn.add(mapper.readValue(rental, RentalDAO.class).toRental());
            }

            if (toReturn.isEmpty()) {
                toReturn = db.getHouseRentals(houseId).stream().map(RentalDAO::toRental).toList();
                for (Rental rental : toReturn) {
                    Cache.addToListInCache(mapper.writeValueAsString(rental), HOUSE_RENTALS);
                }
            }

            return sendResponse(OK, toReturn.isEmpty() ? new ArrayList<>() : toReturn);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode());
        } catch (JsonProcessingException e) {
            return processException(500);
        }
    }


    @Override
    public Response getHousesInDiscount() {
        //TODO: make an azure function to updated the cache in x time
        try {
            List<HouseDAO> houses = new ArrayList<>();
            boolean updateCache = false;

            var discountedHouses = Cache.getListFromCache(DISCOUNTED_HOUSES);
            for (String house : discountedHouses) {
                houses.add(mapper.readValue(house, HouseDAO.class));
            }

            if (houses.isEmpty()) {
                houses = db.getAll(HousesService.CONTAINER, HouseDAO.class).stream().toList();
                updateCache = true;
            }

            var result = new ArrayList<House>();

            for (HouseDAO house : houses) {
                if (!house.getOwnerId().equals(UsersService.DELETED_USER)) {
                    var currentDate = Date.from(Instant.now());
                    var oneMonthFromNow = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));

                    if (house.getDiscount() > 0 && Validations.isAvailable(house.getId(), currentDate, oneMonthFromNow)) {
                        result.add(house.toHouse());
                        if (updateCache)
                            Cache.addToListInCache(house, DISCOUNTED_HOUSES);
                    }
                }
            }

            return sendResponse(OK, result);

        } catch (JsonProcessingException e) {
            return processException(500, "Error while parsing questions");
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

        var checkCookies = checkUserSession(session, house.getOwnerId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        var newPrice = rental.getPrice();
        if (newPrice != null)
            if (newPrice > 0)
                rentalDAO.setPrice(newPrice);
            else
                throw new WebApplicationException("Error: Invalid price", Response.Status.BAD_REQUEST);

        return rentalDAO;
    }

    /**
     * Checks if the Rental can be created
     *
     * @param houseId - id of the house
     * @param rental  - the rental
     * @throws Exception - WebApplicationException depending on the result of the checks
     */
    private HouseDAO checkRentalCreation(Cookie session, String houseId, RentalDAO rental) throws Exception {
        if (Validations.userExists(rental.getUserId()) == null)
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);

        var checkCookies = checkUserSession(session, rental.getUserId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        if (Validations.badParams(rental.getId(), rental.getHouseId(), rental.getUserId())
                || !houseId.equals(rental.getHouseId())
                || rental.getInitialDate().after(rental.getEndDate()))
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

}
