package scc.srv.houses;

import com.azure.cosmos.CosmosException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.utils.Utility;
import scc.srv.utils.Validations;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static scc.srv.utils.Utility.*;

public class HousesResource extends Validations implements HousesService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public Response createHouse(Cookie session, HouseDAO houseDAO) throws Exception {

        try {
            checkHouseCreation(session, houseDAO);

            db.create(houseDAO, CONTAINER);

            Cache.putInCache(houseDAO, HOUSE_PREFIX);

            return sendResponse(OK, houseDAO.toHouse());

        } catch (CosmosException ex) {
            return handleCreateException(ex.getStatusCode(), ex.getMessage(), houseDAO);
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), houseDAO);
        }
    }

    private Response handleCreateException(int statusCode, String msg, HouseDAO houseDAO) {
        if (statusCode == 409)
            return sendResponse(CONFLICT, HOUSE_MSG, houseDAO.getId());
        if (msg.contains(MEDIA_MSG))
            return processException(statusCode, MEDIA_MSG, "(some id)");
        else if (msg.contains(USER_MSG))
            return processException(statusCode, USER_MSG, houseDAO.getOwnerId());
        else if (msg.contains(HOUSE_MSG))
            return processException(statusCode, HOUSE_MSG, houseDAO.getId());
        else
            return processException(statusCode, msg);
    }

    private Response handleUpdateException(int statusCode, String msg, String id) {
        if (msg.contains(MEDIA_MSG))
            return processException(statusCode, MEDIA_MSG, "(some id)");
        else if (msg.contains(HOUSE_MSG))
            return processException(statusCode, HOUSE_MSG, id);
        else
            return processException(statusCode, msg, id);
    }

    @Override
    public Response deleteHouse(Cookie session, String id) throws Exception {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var house = Validations.houseExists(id);
            if (house == null)
                return sendResponse(NOT_FOUND, HOUSE_MSG, id);

            String ownerId = house.getOwnerId();

            var checkCookies = checkUserSession(session, ownerId);
            if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
                return checkCookies;

            db.delete(id, CONTAINER, id);

            var user = Validations.userExists(ownerId);
            if (user == null)
                return sendResponse(NOT_FOUND, USER_MSG, ownerId);

            Cache.deleteFromCache(HOUSE_PREFIX, id);

            return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, HOUSE_MSG, id));

        } catch (CosmosException ex) {
            // throw new Exception("Error " + ex.getStatusCode() + " " + ex.getMessage());
            return processException(ex.getStatusCode(), ex.getMessage(), id/*, ownerId*/); // adicionar aqui o ownerId quando tivermos as
            // cookies
        }
    }

    @Override
    public Response getHouse(String id) throws Exception {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var house = Validations.houseExists(id);
            if (house == null)
                return sendResponse(NOT_FOUND, HOUSE_MSG, id);

            Cache.putInCache(house, HOUSE_PREFIX);

            return sendResponse(OK, house.toHouse());

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    @Override
    public Response updateHouse(Cookie session, String id, House house) throws Exception {

        try {
            var updatedHouse = genUpdatedHouse(session, id, house);

            db.update(updatedHouse, CONTAINER, updatedHouse.getId());

            Cache.putInCache(updatedHouse, HOUSE_PREFIX);

            return sendResponse(OK, updatedHouse.toHouse());

        } catch (CosmosException ex) {
            return handleUpdateException(ex.getStatusCode(), ex.getMessage(), id);
        } catch (WebApplicationException ex) {
            return handleUpdateException(ex.getResponse().getStatus(), ex.getMessage(), id);
        }
    }

    @Override
    public Response listAllHouses() {
        try {
            List<House> toReturn = db.getAll(CONTAINER, HouseDAO.class).stream().map(HouseDAO::toHouse).toList();

            return sendResponse(OK, toReturn.isEmpty() ? new ArrayList<>() : toReturn);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode());
        }
    }

    @Override
    public Response getAvailHouseByLocation(String location) {
        //TODO: chache

        try {
            var houses = db.getHousesByLocation(location);
            var availableHouses = new ArrayList<>();
            Date currentDate = Date.from(Instant.now());

            for (HouseDAO house : houses) {
                /*var rentals = db.getHouseRentals(house.getId());
                boolean isAvailable = true;
                for (RentalDAO rental : rentals) {
                    if (Utility.datesOverlap(currentDate, rental)) {
                        isAvailable = false;
                        break;
                    }
                }
                if (isAvailable)
                    availableHouses.add(house.toHouse());*/
                if (Validations.isAvailable(house.getId(), currentDate, currentDate))
                    availableHouses.add(house.toHouse());
            }

            return sendResponse(OK, availableHouses);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

  /*  @Override
    public List<House> getHouseByLocationPeriod(String location, String initialDate, String endDate) throws Exception {
        //TODO: chache
        Date iniDate = Date.from(Instant.parse(initialDate));
        Date eDate = Date.from(Instant.parse(endDate));

        List<House> result = new ArrayList<>();

        var houses = db.getHousesByLocation(location).stream().toList();

        // Check if house is available in the given timeframe
        for (HouseDAO h : houses) {
            boolean available = true;
            for (String id : h.getRentalsIds()) {
                var rental = db.getRentalById(h.getId(), id).stream().findFirst();
                if (rental.isEmpty()) {
                    available = false;
                } else {
                    RentalDAO r = rental.get();
                    available = r.getInitialDate().after(eDate) || r.getEndDate().before(iniDate);
                }

            }
            if (available)
                result.add(h.toHouse());
        }

        if (!result.isEmpty()) {
            return result;
        } else {
            throw new Exception("Error: 404");
        }
    }*/

    @Override
    public Response getHouseByLocationPeriod(String location, String initialDate, String endDate) {
        //TODO: chache
        Date start = Date.from(Instant.parse(initialDate));
        Date end = Date.from(Instant.parse(endDate));

        try {
            var houses = db.getHousesByLocation(location);
            var availableHouses = new ArrayList<>();

            for (HouseDAO house : houses) {
                if (Validations.isAvailable(house.getId(), start, end))
                    availableHouses.add(house.toHouse());
            }

            return sendResponse(OK, availableHouses);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    /**
     * Returns updated houseDAO to the method who's making the request to the database
     *
     * @param id    of the house being accessed
     * @param house new house attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws WebApplicationException If id is null or if the user does not exist
     */
    private HouseDAO genUpdatedHouse(Cookie session, String id, House house) throws Exception {
        if (Validations.badParams(id))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        var houseDAO = Validations.houseExists(id);
        if (houseDAO == null)
            throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);

        var checkCookies = checkUserSession(session, houseDAO.getOwnerId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        String newName = house.getName();
        if (!newName.isBlank())
            houseDAO.setName(newName);

        String newLocation = house.getLocation();
        if (!newLocation.isBlank())
            houseDAO.setLocation(newLocation);

        var newPhotos = house.getPhotosIds();
        if (!newPhotos.isEmpty())
            if (!Validations.mediaExists(newPhotos))
                throw new WebApplicationException(MEDIA_MSG, Response.Status.NOT_FOUND);
            else
                houseDAO.setPhotosIds(newPhotos);

        String newDescription = house.getDescription();
        if (!newDescription.isBlank())
            houseDAO.setDescription(newDescription);

        var newPrice = house.getPrice();
        if (newPrice != null)
            if (newPrice > 0)
                houseDAO.setPrice(newPrice);
            else
                throw new WebApplicationException("Error: Invalid price", Response.Status.BAD_REQUEST);

        var newDiscount = house.getDiscount();
        if (newDiscount != null)
            if (newDiscount >= 0)
                houseDAO.setDiscount(newDiscount);
            else
                throw new WebApplicationException("Error: Invalid discount", Response.Status.BAD_REQUEST);

        return houseDAO;

    }

    private void checkHouseCreation(Cookie session, HouseDAO houseDAO) throws Exception {
        var checkCookies = checkUserSession(session, houseDAO.getOwnerId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        if (Validations.badParams(houseDAO.getId(), houseDAO.getName(), houseDAO.getLocation(), houseDAO.getPrice().toString(),
                houseDAO.getDiscount().toString()) || houseDAO.getPrice() <= 0 || houseDAO.getDiscount() < 0)
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        if (!Validations.mediaExists(houseDAO.getPhotosIds()))
            throw new WebApplicationException(MEDIA_MSG, Response.Status.NOT_FOUND);

        if (Validations.userExists(houseDAO.getOwnerId()) == null)
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);
    }

    @Override
    public List<String> getNewHouses() throws Exception {
        return Cache.getListFromCache("houses:");
        /*var jsonHouses = Cache.getListFromCache("houses:");
        List<HouseDAO> houses = new ArrayList<>();
        for (String h: jsonHouses) {
            houses.add(mapper.readValue(h,HouseDAO.class));
        }
        return houses;*/
    }
}
