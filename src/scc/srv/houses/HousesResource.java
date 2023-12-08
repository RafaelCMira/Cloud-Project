package scc.srv.houses;


import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.MongoDBLayer;
import scc.srv.utils.Validations;

import java.util.*;

import static scc.srv.utils.Utility.*;

public class HousesResource extends Validations implements HousesService {

    private final MongoDBLayer db = MongoDBLayer.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createHouse(Cookie session, HouseDAO houseDAO) throws Exception {
        try {
            houseDAO.setId(UUID.randomUUID().toString());
            checkHouseCreation(session, houseDAO);

            if (Validations.houseExists(houseDAO.getId()) != null)
                return sendResponse(CONFLICT, HOUSE_MSG, houseDAO.getId());

            houseDAO.setRentalsCounter(0);

            db.create(houseDAO, HousesService.COLLECTION);

            Cache.putInCache(houseDAO, HOUSE_PREFIX);

            return sendResponse(OK, houseDAO.toHouse());

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), houseDAO);
        }
    }

    private void checkHouseCreation(Cookie session, HouseDAO houseDAO) throws Exception {
        var checkCookies = checkUserSession(session, houseDAO.getOwnerId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        if (Validations.badParams(houseDAO.getName(), houseDAO.getLocation(), houseDAO.getPrice().toString(),
                houseDAO.getDiscount().toString()) || houseDAO.getPrice() <= 0 || houseDAO.getDiscount() < 0 || houseDAO.getDiscount() >= houseDAO.getPrice())
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        if (!Validations.mediaExists(houseDAO.getPhotosIds()))
            throw new WebApplicationException(MEDIA_MSG, Response.Status.NOT_FOUND);

        if (Validations.userExists(houseDAO.getOwnerId()) == null)
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);
    }

    @Override
    public Response deleteHouse(Cookie session, String id) throws Exception {
        try {
            var checks = checkHouseDeletion(session, id);
            if (checks.getStatus() != Response.Status.OK.getStatusCode())
                return checks;

            db.delete(id, HousesService.COLLECTION);

            Cache.deleteFromCache(HOUSE_PREFIX, id);

            deleteHouseRentals(id);

            return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, HOUSE_MSG, id));

        } catch (MongoException ex) {
            return processException(ex.getCode(), ex.getMessage(), id);
        }
    }

    private void deleteHouseRentals(String id) {
        //todo
        /*CompletableFuture.runAsync(() -> {
            var houseRentals = db.getAllHouseRentals(id);
            for (var rental : houseRentals) {
                db.delete(rental.getId(), RentalService.COLLECTION, rental.getHouseId());
            }
            Cache.deleteAllFromCache(RentalService.RENTAL_PREFIX, houseRentals.stream().map(RentalDAO::getId).toList());
        });*/
    }

    private Response checkHouseDeletion(Cookie session, String id) throws Exception {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        var house = Validations.houseExists(id);
        if (house == null)
            return sendResponse(NOT_FOUND, HOUSE_MSG, id);

        String ownerId = house.getOwnerId();

        var checkCookies = checkUserSession(session, ownerId);
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            return checkCookies;

        var user = Validations.userExists(ownerId);
        if (user == null)
            return sendResponse(NOT_FOUND, USER_MSG, ownerId);

        return Response.status(Response.Status.OK).build();
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

            loadHouse5MostRecentQuestions(id);

            return sendResponse(OK, house.toHouse());

        } catch (MongoException ex) {
            return processException(ex.getCode(), ex.getMessage());
        }
    }

    private void loadHouse5MostRecentQuestions(String houseId) {
        //TODO
        /*CompletableFuture.runAsync(() -> {
            var questions = db.getHouseQuestions(houseId, "0").stream().map(QuestionDAO::toQuestion).toList();
            try {
                String key = String.format(QuestionService.QUESTIONS_LIST_PREFIX, houseId, "0");
                Cache.putListInCache(questions, key);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });*/
    }

    @Override
    public Response updateHouse(Cookie session, String id, House house) throws Exception {
        try {
            var updatedHouse = genUpdatedHouse(session, id, house);

            db.update(id, updatedHouse, COLLECTION);

            Cache.putInCache(updatedHouse, HOUSE_PREFIX);

            return sendResponse(OK, updatedHouse.toHouse());

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (WebApplicationException ex) {
            return handleUpdateException(ex.getResponse().getStatus(), ex.getMessage(), id);
        }
    }

    @Override
    public Response listAllHouses() {
        try {
            var housesDocuments = db.getAll(HousesService.COLLECTION);

            var houses = new ArrayList<>();

            for (var doc : housesDocuments) {
                houses.add(HouseDAO.fromDocument(doc));
            }

            return sendResponse(OK, houses);

        } catch (MongoException ex) {
            return processException(ex.getCode());
        }
    }

    @Override
    public Response getAvailableHouseByLocation(String location, String offset) {
        //TODO
        /*try {
            List<House> houses = new ArrayList<>();

            String key = String.format(HOUSES_BY_LOCATION_PREFIX, location, offset);
            var cacheHouses = Cache.getListFromCache(key);
            if (!cacheHouses.isEmpty()) {
                for (var house : cacheHouses) {
                    houses.add(mapper.readValue(house, House.class));
                }
                return sendResponse(OK, houses);
            }

            houses = db.getHousesByLocation(location, offset).stream().map(HouseDAO::toHouse).toList();

            List<House> availableHouses = new ArrayList<>();
            Date currentDate = Date.from(Instant.now());

            for (var house : houses) {
                if (Validations.isAvailable(house.getId(), currentDate, currentDate))
                    availableHouses.add(house);
            }

            Cache.putListInCache(availableHouses, key);

            return sendResponse(OK, availableHouses);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }*/
        return null;
    }

    @Override
    public Response getHouseByLocationPeriod(String location, String initialDate, String endDate, String offset) {
        //todo
        /*try {
            var houses = db.getHousesByLocation(location, offset);
            var availableHouses = new ArrayList<>();

            for (HouseDAO house : houses) {
                if (Validations.isAvailable(house.getId(), Utility.formatDate(initialDate), Utility.formatDate(endDate)))
                    availableHouses.add(house.toHouse());
            }

            return sendResponse(OK, availableHouses);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }*/
        return null;
    }

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
                throw new WebApplicationException(INVALID_PRICE, Response.Status.BAD_REQUEST);

        var newDiscount = house.getDiscount();
        if (newDiscount != null)
            if (newDiscount >= 0 && newDiscount < house.getPrice())
                houseDAO.setDiscount(newDiscount);
            else
                throw new WebApplicationException(INVALID_DISCOUNT, Response.Status.BAD_REQUEST);

        return houseDAO;

    }

    @Override
    public Response getNewHouses() {
        //TODO
        /*try {
            var cacheHouses = Cache.getListFromCache(NEW_HOUSES_PREFIX);

            List<HouseDAO> houses = new ArrayList<>();
            for (String house : cacheHouses) {
                houses.add(mapper.readValue(house, HouseDAO.class));
            }

            return sendResponse(OK, houses);

        } catch (JsonProcessingException e) {
            return processException(500);
        }*/
        return null;
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

}
