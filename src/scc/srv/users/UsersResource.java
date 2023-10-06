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
import scc.srv.media.MediaService;

import java.util.Optional;


public class UsersResource implements UsersService {
    private static final String CONTAINER_NAME = "users";
    private final CosmosDBLayer db = CosmosDBLayer.getInstance(CONTAINER_NAME);

    @Override
    public String createUser(UserDAO userDAO) throws Exception {
        // Get container client
        BlobContainerClient containerClient = new BlobContainerClientBuilder()
                .connectionString(MediaService.storageConnectionString)
                .containerName("images")
                .buildClient();

        // Get client to blob
        BlobClient blob = containerClient.getBlobClient(userDAO.getPhotoId());

        // Download contents to BinaryData (check documentation for other alternatives)
        BinaryData data = blob.downloadContent();
        if (data == null) throw new Exception("Error: 404");

        var res = db.createUser(userDAO);
        int statusCode = res.getStatusCode();
        if (isStatusOk(statusCode)) {
            return userDAO.toUser().toString();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public String deleteUser(String id) throws Exception {
        CosmosItemResponse<Object> res = db.delUserById(id);
        int statusCode = res.getStatusCode();
        if (isStatusOk(statusCode)) {
            return String.format("StatusCode: %d \nUser %s was delete", statusCode, id);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public User updateUser(String id, UserDAO userDAO) throws Exception {
        var res = db.updateUserById(id, userDAO);
        int statusCode = res.getStatusCode();
        if (isStatusOk(res.getStatusCode())) {
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

    // Verifies if HTTP code is OK
    private boolean isStatusOk(int statusCode) {
        return statusCode > 200 && statusCode < 300;
    }


}
