package scc.srv.users;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.utils.Cache;
import scc.srv.media.MediaResource;
import scc.utils.Hash;

import java.util.ArrayList;
import java.util.List;

import static scc.srv.utils.Utility.*;


public class UsersResource implements UsersService {

    public static final String PARTITION_KEY = "/id";
    public static final String CONTAINER = "users";
    private final ObjectMapper mapper = new ObjectMapper();
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public Response createUser(UserDAO userDAO) throws Exception {
        if (badParams(userDAO.getId(), userDAO.getName(), userDAO.getPwd(), userDAO.getPhotoId()))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        MediaResource media = new MediaResource();
        if (!media.hasPhotos(List.of(userDAO.getPhotoId())))
            return sendResponse(NOT_FOUND, MEDIA_MSG, "(some id)");

        try {
            db.createItem(userDAO, CONTAINER);

            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                Cache.putInCache(userDAO, USER_PREFIX, jedis);
            }

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, userDAO.getId());
        }

        return sendResponse(OK, userDAO.toUser().toString());
    }

    @Override
    public Response deleteUser(String id) {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        //TODO: colocar "Deleted User" no ownerId das casas do user eliminado e nos Rentals
        //TODO: Usar azure functions

        try {
            db.deleteUser(id);

            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                jedis.del(USER_PREFIX + id);
            }

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, id);
        }
        return sendResponse(OK, String.format(RESOURCE_WAS_DELETED, USER_MSG, id));
    }

    @Override
    public Response getUser(String id) throws JsonProcessingException {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String cacheRes = jedis.get(USER_PREFIX + id);
            if (cacheRes != null)
                return sendResponse(OK, mapper.readValue(cacheRes, UserDAO.class).toUser());

            var result = db.getById(id, CONTAINER, UserDAO.class).stream().findFirst();
            if (result.isPresent()) {
                var user = result.get();
                Cache.putInCache(user, USER_PREFIX, jedis);
                return sendResponse(OK, user.toUser());
            } else
                return sendResponse(NOT_FOUND, USER_MSG, id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, id);
        }
    }

    @Override
    public Response updateUser(String id, User user) throws JsonProcessingException {
        try {

            var updatedUser = genUpdatedUserDAO(id, user);
            db.updateUser(updatedUser);

            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                Cache.putInCache(updatedUser, USER_PREFIX, jedis);
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

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String user = Cache.getFromCache(USER_PREFIX, id, jedis);
            if (user != null)
                return sendResponse(OK, mapper.readValue(user, UserDAO.class).getHouseIds());

            var res = db.getById(id, CONTAINER, UserDAO.class).stream().findFirst();
            if (res.isPresent()) {
                var dbUser = res.get();
                Cache.putInCache(dbUser, USER_PREFIX, jedis);
                return sendResponse(OK, dbUser.getHouseIds());
            } else
                return sendResponse(NOT_FOUND, USER_MSG, id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), USER_MSG, id);
        }
    }


    /**
     * * Returns updated userDAO to the method who's making the request to the database
     *
     * @param id   of the user being accessed
     * @param user new user attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws WebApplicationException If user doesn't exist or if id is empty
     */
    private UserDAO genUpdatedUserDAO(String id, User user) throws WebApplicationException {
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
