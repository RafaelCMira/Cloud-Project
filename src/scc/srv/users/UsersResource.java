package scc.srv.users;

import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.Checks;
import scc.srv.media.MediaResource;
import scc.utils.Hash;

import java.util.List;
import java.util.Optional;


public class UsersResource implements UsersService {
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public String createUser(UserDAO userDAO) throws Exception {
        if (Checks.badParams(userDAO.getUserID(), userDAO.getName(), userDAO.getPwd(), userDAO.getPhotoId()))
            throw new Exception("Error: 400 Bad Request");

        MediaResource media = new MediaResource();
        if (media.hasPhotoById(userDAO.getPhotoId()))
            throw new Exception("Error: 404 Image not found");

        var res = db.createUser(userDAO);
        int statusCode = res.getStatusCode();

        if (Checks.isStatusOk(statusCode))
            return userDAO.toUser().toString();
        else
            throw new Exception("Error: " + statusCode);
    }

    @Override
    public String deleteUser(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosItemResponse<Object> res = db.delUserById(id);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(statusCode))
            return String.format("StatusCode: %d \nUser %s was delete", statusCode, id);
        else
            throw new Exception("Error: " + statusCode);
    }

    @Override
    public User getUser(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosPagedIterable<UserDAO> res = db.getUserById(id);
        Optional<UserDAO> result = res.stream().findFirst();
        if (result.isPresent())
            return result.get().toUser();
        else
            throw new Exception("Error: 404");
    }

    @Override
    public User updateUser(String id, UserDAO userDAO) throws Exception {
        var updatedUser = genUpdatedUserDAO(id, userDAO);
        var res = db.updateUserById(id, updatedUser);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(res.getStatusCode()))
            // return res.getItem().toUser();
            return updatedUser.toUser(); // se isto nao estiver bem usar o acima
        else
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
     * @param userDAO new user attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws Exception If id is null or if the user does not exist
     */
    private UserDAO genUpdatedUserDAO(String id, UserDAO userDAO) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosPagedIterable<UserDAO> res = db.getUserById(id);
        Optional<UserDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            UserDAO u = result.get();

            String userDAOName = userDAO.getName();
            if (!u.getName().equals(userDAOName))
                u.setName(userDAOName);

            String userDAOPwd = Hash.of(userDAO.getPwd());
            if (!u.getPwd().equals(userDAOPwd))
                u.setPwd(userDAOName);

            String userDAOPhotoId = userDAO.getPhotoId();
            if (!u.getPhotoId().equals(userDAOPhotoId))
                u.setPhotoId(userDAOPhotoId);

            return u;

        } else {
            throw new Exception("Error: 404");
        }
    }
}
