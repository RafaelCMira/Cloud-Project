package scc.srv.users;

import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;

import java.util.Optional;


public class UsersResource implements UsersService {
    private static final String CONTAINER_NAME = "users";
    private final CosmosDBLayer db = CosmosDBLayer.getInstance(CONTAINER_NAME);

    @Override
    public String createUser(UserDAO userDAO) throws Exception {
        var res = db.createUser(userDAO);
        int statusCode = res.getStatusCode();
        if (statusCode < 300) {
            return userDAO.toUser().toString();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public User deleteUser(String id) throws Exception {
        var res = db.delUserById(id);
        int statusCode = res.getStatusCode();
        if (statusCode < 300) {
            var user = (UserDAO) res.getItem();
            return user.toUser();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public User updateUser(String id, UserDAO userDAO) throws Exception {
        var res = db.updateUserById(id, userDAO);
        int statusCode = res.getStatusCode();
        if (statusCode < 300) {
            return res.getItem().toUser();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public User getUser(String id) throws Exception {
        CosmosPagedIterable<UserDAO> res = db.getUserById(id);
        Optional<UserDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            return result.get().toUser();
        } else {
            throw new Exception("Error: 404");
        }
    }


}
