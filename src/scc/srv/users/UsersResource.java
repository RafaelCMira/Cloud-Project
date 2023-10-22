package scc.srv.users;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.utils.Checks;
import scc.srv.utils.Cache;
import scc.srv.media.MediaResource;
import scc.utils.Hash;

import java.util.List;


public class UsersResource implements UsersService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public String createUser(UserDAO userDAO) throws Exception {
        if (Checks.badParams(userDAO.getId(), userDAO.getName(), userDAO.getPwd(), userDAO.getPhotoId()))
            throw new Exception("Error: 400 Bad Request");

        MediaResource media = new MediaResource();
        if (!media.hasPhotoById(userDAO.getPhotoId()))
            throw new Exception("Error: 404 Image not found");

        var res = db.createUser(userDAO);
        int statusCode = res.getStatusCode();

        if (Checks.isStatusOk(statusCode)) {
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                // putUserInCache(userDAO, jedis);
                Cache.putInCache(userDAO, USER_PREFIX, jedis);
                return userDAO.toUser().toString();
            }
        } else
            throw new Exception("Error: " + statusCode);
    }

    @Override
    public String deleteUser(String id) throws Exception {
        if (Checks.badParams(id))
            throw new Exception("Error: 400 Bad Request (ID NULL)");

        var res = db.delUserById(id);
        int statusCode = res.getStatusCode();

        if (Checks.isStatusOk(statusCode)) {
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                jedis.del(USER_PREFIX + id);
                return String.format("StatusCode: %d \nUser %s was delete", statusCode, id);
            }
        } else
            throw new Exception("Error: " + statusCode);
    }

    @Override
    public User getUser(String id) throws Exception {
        if (Checks.badParams(id))
            throw new Exception("Error: 400 Bad Request (ID NULL)");

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String userString = jedis.get(USER_PREFIX + id);
            if (userString != null)
                return mapper.readValue(userString, User.class);

            var result = db.getUserById(id).stream().findFirst();
            if (result.isPresent()) {
                var user = result.get();
                Cache.putInCache(user, USER_PREFIX, jedis);
                return user.toUser();
            } else
                throw new Exception("Error: 404");
        }
    }

    @Override
    public User updateUser(String id, User user) throws Exception {
        var updatedUserDAO = genUpdatedUserDAO(id, user);
        var res = db.updateUserById(id, updatedUserDAO);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(statusCode)) {
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                Cache.putInCache(updatedUserDAO, USER_PREFIX, jedis);
                return updatedUserDAO.toUser();
            }
        } else
            throw new Exception("Error: " + statusCode);
    }

    @Override
    public List<User> listUsers() throws Exception {
        return db.listUsers().stream().map(UserDAO::toUser).toList();
    }

    @Override
    public List<String> getUserHouses(String id) throws Exception {
        if (Checks.badParams(id))
            throw new Exception("Error: 400 Bad Request (ID NULL)");

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String user = Cache.getFromCache(USER_PREFIX, id, jedis);
            if (user != null) {
                return mapper.readValue(user, User.class).getHouseIds();
            }

            var res = db.getUserById(id).stream().findFirst();
            if (res.isPresent()) {
                var dbUser = res.get();
                //putListInCache(USER_HOUSES_PREFIX + id, houses, jedis);
                Cache.putInCache(dbUser, USER_PREFIX, jedis);
                return dbUser.getHouseIds();
            } else
                throw new Exception("Error: 404");
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
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosPagedIterable<UserDAO> res = db.getUserById(id);
        var result = res.stream().findFirst();

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
            if (!media.hasPhotoById(newPhoto))
                throw new Exception("Error: 404 Image not found");
            else
                userDAO.setPhotoId(newPhoto);

            return userDAO;
        } else {
            throw new Exception("Error: 404");
        }
    }

    // Usar se for necessario guardar a lista das casas do user
    private void putListInCache(String prefix, List<String> list, Jedis jedis) throws JsonProcessingException {
        jedis.set(prefix, mapper.writeValueAsString(list));
    }

}
