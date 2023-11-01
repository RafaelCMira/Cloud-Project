package scc.srv.users;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.Cache;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.media.MediaResource;
import scc.srv.authentication.Login;
import scc.srv.authentication.Session;
import scc.utils.Hash;
import scc.utils.mgt.AzureManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static scc.srv.utils.Utility.*;


public class UsersResource implements UsersService {

    public static final String PARTITION_KEY = "/id";
    public static final String CONTAINER = "users";
    private final ObjectMapper mapper = new ObjectMapper();
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public Response auth(Login credentials) throws Exception {
        if (badParams(credentials.getId(), credentials.getPwd()))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        String id = credentials.getId();

        var res = getUser(id);
        if (res.getStatus() != Response.Status.OK.getStatusCode())
            return res;

        var user = (User) res.getEntity();

        if (!Hash.of(credentials.getPwd()).equals(user.getPwd()))
            return sendResponse(UNAUTHORIZED, "Incorrect login");

        String uid = UUID.randomUUID().toString();
        NewCookie cookie = new NewCookie.Builder(Session.SESSION)
                .value(id)
                .path("/")
                .comment("sessionid")
                .maxAge(3600)
                .secure(false)
                .httpOnly(true)
                .build();

        if (AzureManagement.CREATE_REDIS) {
            Cache.putInCache(new Session(uid, id), Session.SESSION_PREFIX);
        }

        return Response.ok().cookie(cookie).build();
    }

    @Override
    public Response createUser(UserDAO userDAO) throws Exception {
        if (badParams(userDAO.getId(), userDAO.getName(), userDAO.getPwd(), userDAO.getPhotoId()))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        MediaResource media = new MediaResource();
        if (!media.hasPhotos(List.of(userDAO.getPhotoId())))
            return sendResponse(NOT_FOUND, MEDIA_MSG, "(some id)");

        try {
            userDAO.setPwd(Hash.of(userDAO.getPwd()));
            db.createItem(userDAO, CONTAINER);

            Cache.putInCache(userDAO, USER_PREFIX);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, userDAO.getId());
        }

        return sendResponse(OK, userDAO.toUser());
    }

    @Override
    public Response deleteUser(Cookie session, String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        //TODO: colocar "Deleted User" no ownerId das casas do user eliminado e nos Rentals
        //TODO: Usar azure functions

        var checkCookies = checkUserSession(session, id);
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            return checkCookies;

        try {

            db.deleteUser(id);

            if (AzureManagement.CREATE_REDIS)
                Cache.deleteFromCache(USER_PREFIX, id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, id);
        }

        return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, USER_MSG, id));
    }

    @Override
    public Response getUser(String id) throws JsonProcessingException {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            if (AzureManagement.CREATE_REDIS) {
                String cacheRes = Cache.getFromCache(USER_PREFIX, id);
                if (cacheRes != null)
                    return sendResponse(OK, mapper.readValue(cacheRes, UserDAO.class).toUser());
            }

            var result = db.getById(id, CONTAINER, UserDAO.class).stream().findFirst();
            if (result.isPresent()) {
                var user = result.get();

                if (AzureManagement.CREATE_REDIS) {
                    Cache.putInCache(user, USER_PREFIX);
                }

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
            db.updateUser(updatedUser);

            if (AzureManagement.CREATE_REDIS) {
                Cache.putInCache(updatedUser, USER_PREFIX);
            }

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
            List<User> toReturn = db.getItems(CONTAINER, UserDAO.class).stream().map(UserDAO::toUser).toList();

            return sendResponse(OK, toReturn.isEmpty() ? new ArrayList<>() : toReturn);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode());
        }
    }

    @Override
    public Response getUserHouses(String id) throws JsonProcessingException {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            if (AzureManagement.CREATE_REDIS) {
                String user = Cache.getFromCache(USER_PREFIX, id);
                if (user != null)
                    return sendResponse(OK, mapper.readValue(user, UserDAO.class).getHouseIds());
            }

            var res = db.getById(id, CONTAINER, UserDAO.class).stream().findFirst();
            if (res.isPresent()) {
                var dbUser = res.get();

                if (AzureManagement.CREATE_REDIS) {
                    Cache.putInCache(dbUser, USER_PREFIX);
                }

                return sendResponse(OK, dbUser.getHouseIds());

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
    private UserDAO genUpdatedUserDAO(String id, User user) throws WebApplicationException, JsonProcessingException {
        if (badParams(id))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        var result = db.getById(id, CONTAINER, UserDAO.class).stream().findFirst();
        if (result.isPresent()) {
            UserDAO userDAO = result.get();

            String newName = user.getName();
            if (!newName.isBlank())
                userDAO.setName(newName);

            String newPwd = Hash.of(user.getPwd());
            if (!newPwd.isBlank())
                userDAO.setPwd(newName);

            String newPhoto = user.getPhotoId();
            MediaResource media = new MediaResource();
            if (!newPhoto.isEmpty())
                if (!media.hasPhotos(List.of(newPhoto)))
                    throw new WebApplicationException(MEDIA_MSG, Response.Status.NOT_FOUND);
                else
                    userDAO.setPhotoId(newPhoto);

            return userDAO;

        } else
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);
    }

    // Usar se for necessario guardar a lista das casas do user
    private void putListInCache(String prefix, List<String> list, Jedis jedis) throws JsonProcessingException {
        jedis.set(prefix, mapper.writeValueAsString(list));
    }

}
