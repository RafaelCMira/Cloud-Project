package scc.srv.users;

import com.azure.core.util.BinaryData;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.Checks;
import scc.srv.media.MediaService;
import scc.utils.Hash;

import java.util.Optional;


public class UsersResource implements UsersService {
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public String createUser(UserDAO userDAO) throws Exception {
        if (Checks.badParams(userDAO.getId(), userDAO.getName(), userDAO.getPwd(), userDAO.getPhotoId()))
            throw new Exception("Error: 400 Bad Request");

        // TODO - Replace this blob access with a Media Resource method
        // Get container client
        BlobContainerClient containerClient = new BlobContainerClientBuilder()
                .connectionString(MediaService.storageConnectionString)
                .containerName("images")
                .buildClient();

        // Get client to blob
        BlobClient blob = containerClient.getBlobClient(userDAO.getPhotoId());

        // Download contents to BinaryData (check documentation for other alternatives)
        BinaryData data = blob.downloadContent();
        if (data == null) throw new Exception("Error: 404 Image not found");

        var res = db.createUser(userDAO);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(statusCode)) {
            return userDAO.toUser().toString();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public String deleteUser(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosItemResponse<Object> res = db.delUserById(id);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(statusCode)) {
            return String.format("StatusCode: %d \nUser %s was delete", statusCode, id);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public User updateUser(String id, UserDAO userDAO) throws Exception {
        var updatedUser = updatedUser(id, userDAO);
        var res = db.updateUserById(id, updatedUser);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(res.getStatusCode())) {
            // return res.getItem().toUser();
            return updatedUser.toUser(); // se isto nao estiver bem usar o acima
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public User getUser(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosPagedIterable<UserDAO> res = db.getUserById(id);
        Optional<UserDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            return result.get().toUser();
        } else {
            throw new Exception("Error: 404");
        }
    }


    /**
     * Returns updated userDAO to the method who's making the request to the database
     *
     * @param id      of the user being accessed
     * @param userDAO new user attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws Exception If id is null or if the user does not exist
     */
    private UserDAO updatedUser(String id, UserDAO userDAO) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosPagedIterable<UserDAO> res = db.getUserById(id);
        Optional<UserDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            UserDAO u = result.get();

            String userDAOname = userDAO.getName();
            if (!u.getName().equals(userDAOname))
                u.setName(userDAOname);

            String userDAOpwd = Hash.of(userDAO.getPwd());
            if (!u.getPwd().equals(userDAOpwd))
                u.setPwd(userDAOname);

            String userDAOPhotoId = userDAO.getPhotoId();
            if (!u.getPhotoId().equals(userDAOPhotoId))
                u.setPhotoId(userDAOPhotoId);

            return u;

        } else {
            throw new Exception("Error: 404");
        }
    }
}
