package scc.db;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;

import scc.data.UserDAO;

public class CosmosDBLayer {
    private static final String CONNECTION_URL = "https://scc-60700-2324.documents.azure.com:443/";
    private static final String DB_KEY = "X7frCj0reVTuHn39JjLHsgq0M7xLJX1NSsCJAcyUEJOXfovmRy4LUzPRIlorA90Dq3KiDLiAQNEUACDbE8sVxQ==";
    private static final String DB_NAME = "scc607002324";

    private static CosmosDBLayer instance;

    public static synchronized CosmosDBLayer getInstance(String containerName) {
        if (instance != null)
            return instance;

        CosmosClient client = new CosmosClientBuilder()
                .endpoint(CONNECTION_URL)
                .key(DB_KEY)
                .gatewayMode() // replace by .directMode() for better performance
                .consistencyLevel(ConsistencyLevel.SESSION)
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        instance = new CosmosDBLayer(client, containerName);
        return instance;
    }

    private CosmosClient client;
    private CosmosDatabase db;
    private String containerName;
    private CosmosContainer container;

    public CosmosDBLayer(CosmosClient client, String container) {
        this.client = client;
        this.containerName = container;
    }

    private synchronized void init() {
        if (db != null)
            return;
        db = client.getDatabase(DB_NAME);
        container = db.getContainer(containerName);
    }

    public CosmosItemResponse<UserDAO> createUser(UserDAO user) {
        init();
        return container.createItem(user);
    }

    public CosmosItemResponse<Object> delUserById(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        return container.deleteItem(id, key, new CosmosItemRequestOptions());
    }

    public CosmosPagedIterable<UserDAO> getUserById(String id) {
        init();
        return container.queryItems("SELECT * FROM users WHERE users.id=\"" + id + "\"", new CosmosQueryRequestOptions(), UserDAO.class);
    }

    public CosmosItemResponse<UserDAO> updateUserById(String id, UserDAO user) {
        init();
        PartitionKey key = new PartitionKey(id);
        return container.replaceItem(user, id, key, new CosmosItemRequestOptions());
    }

    /*public CosmosItemResponse<UserDAO> updateUserById(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        return container.patchItem(id, key, new CosmosPatchItemRequestOptions(), UserDAO.class);
    }*/


    public CosmosPagedIterable<UserDAO> getContainer() {
        init();
        return container.queryItems("SELECT * FROM users ", new CosmosQueryRequestOptions(), UserDAO.class);
    }

    /*public CosmosItemResponse<Object> delUser(UserDAO user) {
        init();
        return container.deleteItem(user, new CosmosItemRequestOptions());
    }
*/

    public void close() {
        client.close();
    }


}
