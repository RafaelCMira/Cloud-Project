package scc.db;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;

import scc.data.HouseDAO;
import scc.data.RentalDAO;
import scc.data.UserDAO;

public class CosmosDBLayer {

    public static final String USERS_CONTAINER = "users";
    public static final String HOUSES_CONTAINER = "houses";
    public static final String RENTALS_CONTAINER = "rentals";
    public static final String QUESTIONS_CONTAINER = "questions";

    private static final String CONNECTION_URL = System.getenv("COSMOSDB_URL");
    private static final String DB_KEY = System.getenv("COSMOSDB_KEY");
    private static final String DB_NAME = System.getenv("COSMOSDB_DATABASE");

    private static CosmosDBLayer instance;
    private final CosmosClient client;
    private CosmosDatabase db;

    public CosmosDBLayer(CosmosClient client) {
        this.client = client;
    }

    public static synchronized CosmosDBLayer getInstance() {
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
        instance = new CosmosDBLayer(client);
        return instance;
    }

    private synchronized void init() {
        if (db != null)
            return;
        db = client.getDatabase(DB_NAME);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// USERS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosItemResponse<UserDAO> createUser(UserDAO user) {
        init();
        return db.getContainer(USERS_CONTAINER).createItem(user);
    }

    public CosmosItemResponse<Object> delUserById(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        return db.getContainer(USERS_CONTAINER).deleteItem(id, key, new CosmosItemRequestOptions());
    }

    /*public CosmosItemResponse<Object> delUser(UserDAO user) {
        init();
        return container.deleteItem(user, new CosmosItemRequestOptions());
    }*/

    public CosmosPagedIterable<UserDAO> getUserById(String id) {
        init();
        String query = "SELECT * FROM users WHERE users.id=\"" + id + "\"";
        return db.getContainer(USERS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), UserDAO.class);
    }

    public CosmosItemResponse<UserDAO> updateUserById(String id, UserDAO user) {
        init();
        PartitionKey key = new PartitionKey(id);
        return db.getContainer(USERS_CONTAINER).replaceItem(user, id, key, new CosmosItemRequestOptions());
    }

    /*public CosmosItemResponse<UserDAO> updateUserById(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        return container.patchItem(id, key, new CosmosPatchItemRequestOptions(), UserDAO.class);
    }*/

    public CosmosPagedIterable<UserDAO> getUsers() {
        init();
        String query = "SELECT * FROM users";
        return db.getContainer(USERS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), UserDAO.class);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// HOUSES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public CosmosItemResponse<HouseDAO> createHouse(HouseDAO house) {
        init();
        return db.getContainer(HOUSES_CONTAINER).createItem(house);
    }

    public CosmosItemResponse<Object> delHouseById(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        return db.getContainer(HOUSES_CONTAINER).deleteItem(id, key, new CosmosItemRequestOptions());
    }

    public CosmosPagedIterable<HouseDAO> getHouseById(String id) {
        init();
        String query = "SELECT * FROM houses WHERE houses.id=\"" + id + "\"";
        return db.getContainer(HOUSES_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
    }

    public CosmosItemResponse<HouseDAO> updateHouseById(String id, HouseDAO house) {
        init();
        PartitionKey key = new PartitionKey(id);
        return db.getContainer(HOUSES_CONTAINER).replaceItem(house, id, key, new CosmosItemRequestOptions());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// RENTALS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosItemResponse<RentalDAO> createRental(RentalDAO rental) {
        init();
        return db.getContainer(RENTALS_CONTAINER).createItem(rental);
    }

    public CosmosPagedIterable<RentalDAO> getRentalById(String id) {
        init();
        String query = "SELECT * FROM rentals WHERE rentals.id=\"" + id + "\"";
        return db.getContainer(RENTALS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }

    public CosmosPagedIterable<RentalDAO> getRentals() {
        init();
        String query = "SELECT * FROM rentals";
        return db.getContainer(RENTALS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }

    public CosmosItemResponse<RentalDAO> updateRentalById(String houseId, RentalDAO rental) {
        init();
        PartitionKey key = new PartitionKey(houseId);
        return db.getContainer(RENTALS_CONTAINER).replaceItem(rental, houseId, key, new CosmosItemRequestOptions());
    }

    public void close() {
        client.close();
    }


}
