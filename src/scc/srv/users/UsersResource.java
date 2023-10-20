package scc.srv.users;

import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.Checks;
import scc.srv.media.MediaResource;
import scc.srv.media.MediaService;
import scc.utils.Hash;

import java.util.List;
import java.util.Optional;


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

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            if (Checks.isStatusOk(statusCode)) {
                jedis.set(CACHE_PREFIX + userDAO.getId(), mapper.writeValueAsString(userDAO));
                return userDAO.toUser().toString();
            } else
                throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public String deleteUser(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            CosmosItemResponse<Object> res = db.delUserById(id);
            int statusCode = res.getStatusCode();

            if (Checks.isStatusOk(statusCode)) {
                jedis.getDel(CACHE_PREFIX + id);
                return String.format("StatusCode: %d \nUser %s was delete", statusCode, id);
            } else
                throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public User getUser(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String user = jedis.get(CACHE_PREFIX+id);
            if (user != null)
                return mapper.readValue(user,UserDAO.class).toUser();

            CosmosPagedIterable<UserDAO> res = db.getUserById(id);
            Optional<UserDAO> result = res.stream().findFirst();
            if (result.isPresent())
                return result.get().toUser();
            else
                throw new Exception("Error: 404");
        }
    }

    @Override
    public User updateUser(String id, User user) throws Exception {
        var updatedUser = genUpdatedUserDAO(id, user);
        var res = db.updateUserById(id, updatedUser);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(statusCode)) {
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                jedis.set(CACHE_PREFIX+id, mapper.writeValueAsString(updatedUser));
            }
            return res.getItem().toUser();
            //   return updatedUser.toUser(); // se isto nao estiver bem usar o acima
        } else
            throw new Exception("Error: " + statusCode);
    }

    @Override
    public List<User> listUsers() throws Exception {
        return db.listUsers().stream().map(UserDAO::toUser).toList();
    }


    /**
     * Returns updated userDAO to the method who's making the request to the database
     *
     * @param id      of the user being accessed
     * @param user new user attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws Exception If id is null or if the user does not exist
     */
    private UserDAO genUpdatedUserDAO(String id, User user) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        // TODO - Check cache
        CosmosPagedIterable<UserDAO> res = db.getUserById(id);
        Optional<UserDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            UserDAO u = result.get();

            String userDAOName = user.getName();
            if (!u.getName().equals(userDAOName))
                u.setName(userDAOName);

            String userDAOPwd = Hash.of(user.getPwd());
            if (!u.getPwd().equals(userDAOPwd))
                u.setPwd(userDAOName);

            String userDAOPhotoId = user.getPhotoId();
            if (!u.getPhotoId().equals(userDAOPhotoId))
                u.setPhotoId(userDAOPhotoId);

            return u;

        } else {
            throw new Exception("Error: 404");
        }
    }
}
