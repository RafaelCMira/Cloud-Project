package scc.srv.users;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.House;
import scc.data.HouseDAO;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.authentication.Login;
import scc.srv.authentication.Session;
import scc.srv.houses.HousesResource;
import scc.srv.houses.HousesService;
import scc.srv.rentals.RentalService;
import scc.srv.utils.Validations;
import scc.utils.Hash;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static scc.srv.utils.Utility.*;


public class UsersResource extends Validations implements UsersService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

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

        Cache.putInCache(new Session(uid, id), Session.SESSION_PREFIX);

        return Response.ok(user).cookie(cookie).build();
    }

    @Override
    public Response createUser(UserDAO userDAO) throws Exception {
        if (Validations.badParams(userDAO.getId(), userDAO.getName(), userDAO.getPwd(), userDAO.getPhotoId()))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        if (!Validations.mediaExists(List.of(userDAO.getPhotoId())))
            return sendResponse(NOT_FOUND, MEDIA_MSG, "(some id)");

        try {
            var plainPwd = userDAO.getPwd();
            userDAO.setPwd(Hash.of(plainPwd));
            db.create(userDAO, CONTAINER);

            Cache.putInCache(userDAO, USER_PREFIX);

            return auth(new Login(userDAO.getId(), plainPwd));

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, userDAO.getId());
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
            var userHouses = db.getUserHouses(id);
            var userRentals = db.getUserRentals(id);

            var updateHouses = CompletableFuture.runAsync(() -> {
                for (var house : userHouses) {
                    house.setOwnerId(DELETED_USER);
                    db.update(house, HousesService.CONTAINER, house.getId());
                    Cache.deleteFromCache(HousesService.HOUSE_PREFIX, house.getId());
                }
            });

            var updateRentals = CompletableFuture.runAsync(() -> {
                for (var rental : userRentals) {
                    rental.setUserId(DELETED_USER);
                    db.update(rental, RentalService.CONTAINER, rental.getHouseId());
                    Cache.deleteFromCache(RentalService.RENTAL_PREFIX, rental.getId());
                }
            });

            var allUpdates = CompletableFuture.allOf(updateHouses, updateRentals);
            allUpdates.join();

            db.delete(id, CONTAINER, id);

            Cache.deleteFromCache(USER_PREFIX, id);

            return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, USER_MSG, id));

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, id);
        }
    }

    @Override
    public Response getUser(String id) throws JsonProcessingException {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var user = Validations.userExists(id);
            if (user != null) {

                Cache.putInCache(user, USER_PREFIX);

                return sendResponse(OK, user.toUser());

            } else
                return sendResponse(NOT_FOUND, USER_MSG, id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, id);
        }
    }

    @Override
    public Response updateUser(Cookie session, String id, User user) throws Exception {

        try {
            var checkCookies = checkUserSession(session, id);
            if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
                return checkCookies;

            var updatedUser = genUpdatedUserDAO(id, user);

            db.update(updatedUser, CONTAINER, updatedUser.getId());

            Cache.putInCache(updatedUser, USER_PREFIX);

            return sendResponse(OK, updatedUser.toUser());

        } catch (CosmosException ex) {
            return handleUpdateException(ex.getStatusCode(), ex.getMessage(), id);
        } catch (WebApplicationException ex) {
            return handleUpdateException(ex.getResponse().getStatus(), ex.getMessage(), id);
        }
    }

    private Response handleUpdateException(int statusCode, String msg, String id) {
        if (msg.contains(MEDIA_MSG))
            return processException(statusCode, MEDIA_MSG, id);
        if (msg.contains(USER_MSG))
            return processException(statusCode, USER_MSG, id);
        else
            return processException(statusCode, msg, id);
    }

    @Override
    public Response listUsers() {
        try {
            List<User> toReturn = db.getAll(CONTAINER, UserDAO.class).stream().map(UserDAO::toUser).toList();

            return sendResponse(OK, toReturn.isEmpty() ? new ArrayList<>() : toReturn);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode());
        }
    }

    @Override
    public Response getUserHouses(String id) throws JsonProcessingException {
        if (Validations.badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            var user = Validations.userExists(id);
            if (user != null) {

                Cache.putInCache(user, USER_PREFIX);

                var userHouses = db.listUserHouses(id).stream().map(HouseDAO::toHouse).toList();
                return sendResponse(OK, userHouses.isEmpty() ? new ArrayList<>() : userHouses);

            } else
                return sendResponse(NOT_FOUND, USER_MSG, id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, id);
        }
    }


    /**
     * Returns updated userDAO to the method who's making the request to the database
     *
     * @param id   of the user being accessed
     * @param user new user attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws WebApplicationException If user doesn't exist or if id is empty
     */
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

}
