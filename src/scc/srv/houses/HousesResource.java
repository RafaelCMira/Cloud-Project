package scc.srv.houses;

import com.azure.cosmos.CosmosException;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.util.SearchPagedIterable;
import com.azure.search.documents.util.SearchPagedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.cognitiveSearch.CognitiveSearchLayer;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.question.QuestionService;
import scc.srv.rentals.RentalService;
import scc.srv.utils.Utility;
import scc.srv.utils.Validations;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static scc.srv.utils.Utility.*;

public class HousesResource extends Validations implements HousesService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createHouse(Cookie session, HouseDAO houseDAO) throws Exception {
        try {
            houseDAO.setId(UUID.randomUUID().toString());
            checkHouseCreation(session, houseDAO);
            houseDAO.setRentalsCounter(0);

            db.create(houseDAO, CONTAINER);

            Cache.putInCache(houseDAO, HOUSE_PREFIX);

            return sendResponse(OK, houseDAO.toHouse());

        } catch (CosmosException ex) {
            return handleCreateException(ex.getStatusCode(), ex.getMessage(), houseDAO);
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

            db.delete(id, CONTAINER, id);

            Cache.deleteFromCache(HOUSE_PREFIX, id);

            deleteHouseRentals(id);

            return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, HOUSE_MSG, id));

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage(), id);
        }
    }

    private void deleteHouseRentals(String id) {
        CompletableFuture.runAsync(() -> {
            var houseRentals = db.getAllHouseRentals(id);
            for (var rental : houseRentals) {
                db.delete(rental.getId(), RentalService.CONTAINER, rental.getHouseId());
            }
            Cache.deleteAllFromCache(RentalService.RENTAL_PREFIX, houseRentals.stream().map(RentalDAO::getId).toList());
        });
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

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    private void loadHouse5MostRecentQuestions(String houseId) {
        CompletableFuture.runAsync(() -> {
            var questions = db.getHouseQuestions(houseId, "0").stream().map(QuestionDAO::toQuestion).toList();
            try {
                String key = String.format(QuestionService.QUESTIONS_LIST_PREFIX, houseId, "0");
                Cache.putListInCache(questions, key);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
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
            List<House> houses = db.getAll(CONTAINER, HouseDAO.class).stream().map(HouseDAO::toHouse).toList();

            return sendResponse(OK, houses.isEmpty() ? new ArrayList<>() : houses);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode());
        }
    }

    @Override
    public Response getAvailableHouseByLocation(String location, String offset) {
        try {
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
        }
    }

    @Override
    public Response getHouseByLocationPeriod(String location, String initialDate, String endDate, String offset) {
        try {
            var houses = db.getHousesByLocation(location, offset);
            var availableHouses = new ArrayList<>();

            for (HouseDAO house : houses) {
                if (Validations.isAvailable(house.getId(), Utility.formatDate(initialDate), Utility.formatDate(endDate)))
                    availableHouses.add(house.toHouse());
            }

            return sendResponse(OK, availableHouses);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
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
        try {
            var cacheHouses = Cache.getListFromCache(NEW_HOUSES_PREFIX);

            List<HouseDAO> houses = new ArrayList<>();
            for (String house : cacheHouses) {
                houses.add(mapper.readValue(house, HouseDAO.class));
            }

            return sendResponse(OK, houses);

        } catch (JsonProcessingException e) {
            return processException(500);
        }
    }

    @Override
    public Response getHousesByDescription(String description) {
        try {
            SearchOptions options = new SearchOptions()
                    .setIncludeTotalCount(true)
                    .setSearchFields("description")
                    .setTop(5);
            SearchPagedIterable search = CognitiveSearchLayer.getInstance().search(description, options);
            List<Object> houses = new ArrayList<>();

            for (SearchPagedResponse resultResponse : search.iterableByPage()) {
                resultResponse.getValue().forEach(searchResult -> {
                    for (Map.Entry<String, Object> res : searchResult.getDocument(SearchDocument.class).entrySet()) {
                        houses.add(res.getValue());
                    }
                });
            }

            return sendResponse(OK, houses);

        } catch (Exception e) {
            return processException(500);
        }
    }

    @Override
    public Response getHousesByDescAnd(String word, String location) {
        try {
            SearchOptions options = new SearchOptions()
                    .setIncludeTotalCount(true)
                    .setFilter("location eq '" + location + "'")
                    .setSearchFields("description")
                    .setTop(5);
            SearchPagedIterable search = CognitiveSearchLayer.getInstance().search(word, options);
            List<Object> houses = new ArrayList<>();

            for (SearchPagedResponse resultResponse : search.iterableByPage()) {
                resultResponse.getValue().forEach(searchResult -> {
                    for (Map.Entry<String, Object> res : searchResult.getDocument(SearchDocument.class).entrySet()) {
                        houses.add(res.getValue());
                    }
                });
            }

            return sendResponse(OK, houses);

        } catch (Exception e) {
            return processException(500);
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

}
