package scc.srv.users;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.ConflictException;
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
            return sendResponse(BAD_REQUEST);

        MediaResource media = new MediaResource();
        if (!media.hasPhotos(List.of(userDAO.getPhotoId())))
            return sendResponse(NOT_FOUND, "Image", "(some id)");

        try {
            var res = db.createItem(userDAO, CONTAINER);
            int statusCode = res.getStatusCode();
            if (isStatusOk(statusCode)) {
                try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                    Cache.putInCache(userDAO, USER_PREFIX, jedis);
                }
            }
        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), "User", userDAO.getId());
        }

        return sendResponse(OK, userDAO.toUser().toString());
    }

    @Override
    public Response deleteUser(String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST);

        //TODO: colocar "Deleted User" no ownerId das casas do user eliminado e nos Rentals
        //TODO: Usar azure functions

        try {
            var res = db.deleteUser(id);
            int statusCode = res.getStatusCode();
            if (isStatusOk(statusCode)) {
                try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                    jedis.del(USER_PREFIX + id);
                }
            }
        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), "User", id);
        }
        return sendResponse(OK, String.format("User %s was deleted", id));
    }

    @Override
    public Response getUser(String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST);

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
                return sendResponse(NOT_FOUND, "User", id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), "User", id);
        }
    }

    @Override
    public Response updateUser(String id, User user) throws Exception {

        try {
            var updatedUser = genUpdatedUserDAO(id, user);
            db.updateUser(updatedUser);

            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                Cache.putInCache(updatedUser, USER_PREFIX, jedis);
                return sendResponse(OK, updatedUser.toUser());
            }

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), "User", id);
        } catch (WebApplicationException ex) {
            return processException(ex.getResponse().getStatus(), "User", id);
        }
    }

    @Override
    public List<User> listUsers() {
        var res = db.getItems(CONTAINER, UserDAO.class).stream().map(UserDAO::toUser).toList();
        if (!res.isEmpty())
            return res;
        else
            return new ArrayList<>();
    }

    @Override
    public Response getUserHouses(String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST);
        
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
                return sendResponse(NOT_FOUND, "User", id);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), "User", id);
        }
    }


    /**
     * Returns updated userDAO to the method who's making the request to the database
     *
     * @param id   of the user being accessed
     * @param user new user attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws Exception If id is null or if the user does not exist
     */
    private UserDAO genUpdatedUserDAO(String id, User user) throws Exception {
        if (badParams(id))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

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
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                else
                    userDAO.setPhotoId(newPhoto);

            return userDAO;
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    // Usar se for necessario guardar a lista das casas do user
    private void putListInCache(String prefix, List<String> list, Jedis jedis) throws JsonProcessingException {
        jedis.set(prefix, mapper.writeValueAsString(list));
    }

}
