package scc.srv.users;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import scc.cache.Cache;
import scc.data.*;
import scc.db.MongoDBLayer;
import scc.srv.authentication.Login;
import scc.srv.authentication.Session;
import scc.srv.houses.HousesService;
import scc.srv.rentals.RentalService;
import scc.srv.utils.Validations;
import scc.utils.Hash;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static scc.srv.utils.Utility.*;


public class UsersResource extends Validations implements UsersService {

    private final MongoDBLayer db = MongoDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response auth(Login credentials) throws Exception {
        if (Validations.badParams(credentials.getId(), credentials.getPwd()))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        String id = credentials.getId();

        var res = getUser(id);
        if (res.getStatus() != Response.Status.OK.getStatusCode())
            return Response.status(res.getStatus()).entity(res.getEntity()).build();

        var user = (User) res.getEntity();

        if (!Hash.of(credentials.getPwd()).equals(user.getPwd()))
            return sendResponse(UNAUTHORIZED, INCORRECT_LOGIN);

        String uid = UUID.randomUUID().toString();
        NewCookie cookie = new NewCookie.Builder(Session.SESSION)
                .value(id)
                .path("/")
                .comment("sessionid")
                .maxAge(3600)
                .secure(false)
                .httpOnly(true)
                .build();

        Cache.putCookieInCache(new Session(uid, id), Session.SESSION_PREFIX);

        return Response.ok(user).cookie(cookie).build();
    }

    @Override
    public Response createUser(UserDAO userDAO) throws Exception {
        String id = userDAO.getId();

        if (Validations.badParams(id, userDAO.getName(), userDAO.getPwd(), userDAO.getPhotoId()))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        if (Validations.userExists(id) != null)
            return sendResponse(CONFLICT, USER_MSG, userDAO.getId());

        if (!Validations.mediaExists(List.of(userDAO.getPhotoId())))
            return sendResponse(NOT_FOUND, MEDIA_MSG, "(some id)");

        try {
            var plainPwd = userDAO.getPwd();
            userDAO.setPwd(Hash.of(plainPwd));

            db.create(userDAO, UsersService.COLLECTION);

            Cache.putInCache(userDAO, USER_PREFIX);

            return auth(new Login(id, plainPwd));
        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        }
    }

    @Override
    public Response deleteUser(Cookie session, String id) throws Exception {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        var checkCookies = checkUserSession(session, id);
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            return checkCookies;

        try {
            updateUserHousesAndRentals(id);

            db.delete(id, UsersService.COLLECTION);

            Cache.deleteFromCache(USER_PREFIX, id);

            return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, USER_MSG, id));

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        }
    }

    private void updateUserHousesAndRentals(String id) {
        var updateHouses = CompletableFuture.runAsync(() -> {
            var userHouses = db.getAllUserHouses(id);
            for (var house : userHouses) {
                house.setOwnerId(DELETED_USER);
                db.update(house.getId(), house, HousesService.COLLECTION);
                Cache.deleteFromCache(HousesService.HOUSE_PREFIX, house.getId());
            }
        });

        var updateRentals = CompletableFuture.runAsync(() -> {
            var userRentals = db.getAllUserRentals(id);
            for (var rental : userRentals) {
                rental.setUserId(DELETED_USER);
                db.update(rental.getHouseId(), rental, RentalService.COLLECTION);
                Cache.deleteFromCache(RentalService.RENTAL_PREFIX, rental.getId());
            }
        });

        var allUpdates = CompletableFuture.allOf(updateHouses, updateRentals);
        allUpdates.join();
    }

    @Override
    public Response getUser(String id) throws JsonProcessingException {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var user = Validations.userExists(id);

            if (user == null)
                return sendResponse(NOT_FOUND, USER_MSG, id);

            Cache.putInCache(user, USER_PREFIX);

            return sendResponse(OK, user.toUser());

        } catch (MongoException ex) {
            return processException(ex.getCode(), USER_MSG, id);
        }
    }

    @Override
    public Response updateUser(Cookie session, String id, User user) throws Exception {
        try {
            var checkCookies = checkUserSession(session, id);
            if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
                return checkCookies;

            var updatedUser = genUpdatedUserDAO(id, user);

            db.update(id, updatedUser, UsersService.COLLECTION);

            Cache.putInCache(updatedUser, USER_PREFIX);

            return sendResponse(OK, updatedUser.toUser());

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (WebApplicationException ex) {
            return handleUpdateException(ex.getResponse().getStatus(), ex.getMessage(), id);
        }

    }

    @Override
    public Response listUsers() {
        try {
            var usersDocuments = db.getAll(UsersService.COLLECTION);

            var users = new ArrayList<>();

            for (var doc : usersDocuments) {
                users.add(UserDAO.fromDocument(doc));
            }

            return sendResponse(OK, users);

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        }
    }

    @Override
    public Response getUserHouses(String id, int offset) {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var user = Validations.userExists(id);
            if (user != null) {

                var houses = new ArrayList<>();

                String key = String.format(USER_HOUSES_PREFIX, id, offset);
                var cacheHouses = Cache.getListFromCache(key);
                if (!cacheHouses.isEmpty()) {
                    for (var house : cacheHouses) {
                        houses.add(mapper.readValue(house, House.class));
                    }
                    return sendResponse(OK, houses);
                }

                var userHouses = db.listUserHouses(id, offset);
                Cache.putListInCache(userHouses, key);

                return sendResponse(OK, userHouses);

            } else
                return sendResponse(NOT_FOUND, USER_MSG, id);

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Response getUserRentals(String id, int offset) {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var user = Validations.userExists(id);
            if (user != null) {

                var rentals = new ArrayList<>();

                String key = String.format(USER_RENTALS_PREFIX, id, offset);
                var cacheRentals = Cache.getListFromCache(key);
                if (!cacheRentals.isEmpty()) {
                    for (var house : cacheRentals) {
                        rentals.add(mapper.readValue(house, Rental.class));
                    }
                    return sendResponse(OK, rentals);
                }

                var userRentals = db.listUserRentals(id, offset);
                Cache.putListInCache(userRentals, key);

                return sendResponse(OK, userRentals);

            } else
                return sendResponse(NOT_FOUND, USER_MSG, id);

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private UserDAO genUpdatedUserDAO(String id, User user) throws WebApplicationException {
        if (Validations.badParams(id))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        var userDAO = Validations.userExists(id);
        if (userDAO == null)
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);

        String newName = user.getName();
        if (!newName.isBlank())
            userDAO.setName(newName);

        String newPwd = Hash.of(user.getPwd());
        if (!newPwd.isBlank())
            userDAO.setPwd(newPwd);

        String newPhoto = user.getPhotoId();
        if (!newPhoto.isEmpty())
            if (!Validations.mediaExists(List.of(newPhoto)))
                throw new WebApplicationException(MEDIA_MSG, Response.Status.NOT_FOUND);
            else
                userDAO.setPhotoId(newPhoto);

        return userDAO;
    }

    private Response handleUpdateException(int statusCode, String msg, String id) {
        if (msg.contains(MEDIA_MSG))
            return processException(statusCode, MEDIA_MSG, id);
        if (msg.contains(USER_MSG))
            return processException(statusCode, USER_MSG, id);
        else
            return processException(statusCode, msg, id);
    }

}
