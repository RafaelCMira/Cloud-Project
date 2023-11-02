package scc.db;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;

import scc.data.*;
import scc.srv.houses.HousesResource;
import scc.srv.question.QuestionResource;
import scc.srv.rentals.RentalResource;
import scc.srv.users.UsersResource;
import scc.utils.props.AzureProperties;

public class CosmosDBLayer {

    private static final String CONNECTION_URL = System.getenv(AzureProperties.COSMOSDB_URL);
    private static final String DB_KEY = System.getenv(AzureProperties.COSMOSDB_KEY);
    private static final String DB_NAME = System.getenv(AzureProperties.COSMOSDB_DATABASE);

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
                //.directMode()
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
    ////////////////////////////// GENERICS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosItemResponse<Object> createItem(Object item, String container) {
        init();
        return db.getContainer(container).createItem(item);
    }

    public <T> CosmosPagedIterable<T> getById(String id, String container, Class<T> c) {
        init();
        String query = String.format("SELECT * FROM %s WHERE %s.id=\"%s\"", container, container, id);
        return db.getContainer(container).queryItems(query, new CosmosQueryRequestOptions(), c);
    }

    public <T> CosmosPagedIterable<T> getItems(String container, Class<T> c) {
        init();
        String query = String.format("SELECT * FROM %s", container);
        return db.getContainer(container).queryItems(query, new CosmosQueryRequestOptions(), c);
    }

    //  TODO: Nao funcionam (só para não tentar outra vez) DELETE E PU têm de ser feitos em separado para cada
/*    public CosmosItemResponse<Object> updateById(Object item, String id, String container, String partitionKey) {
        init();
        PartitionKey key = new PartitionKey(partitionKey);
        return db.getContainer(container).replaceItem(item, id, key, new CosmosItemRequestOptions());
    }

    public <T> CosmosItemResponse<T> updateById2(T item, String id, String container, String partitionKey) {
        init();
        PartitionKey key = new PartitionKey(partitionKey);
        return db.getContainer(container).replaceItem(item, id, key, new CosmosItemRequestOptions());
    }

    public CosmosItemResponse<Object> deleteById(String id, String container, String partitionKey) throws Exception {
        init();
        PartitionKey key = new PartitionKey(partitionKey);
        return db.getContainer(container).deleteItem(id, key, new CosmosItemRequestOptions());
    }*/


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// USERS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void updateUser(UserDAO user) {
        init();
        var id = user.getId();
        PartitionKey key = new PartitionKey(id);
        db.getContainer(UsersResource.CONTAINER).replaceItem(user, id, key, new CosmosItemRequestOptions());
    }

    public void deleteUser(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        db.getContainer(UsersResource.CONTAINER).deleteItem(id, key, new CosmosItemRequestOptions());
    }

    // TODO: Perguntar se no listar casas de um user podemos só listar o id da casa OU se temos de listar o JSON das casas
    public CosmosPagedIterable<String> listUserHouses(String id) {
        init();
        String query = String.format("SELECT housesIds FROM users WHERE users.id=\"%s\"", id);
        return db.getContainer(UsersResource.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), String.class);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// HOUSES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void updateHouse(HouseDAO house) {
        init();
        var id = house.getId();
        PartitionKey key = new PartitionKey(id);
        db.getContainer(HousesResource.CONTAINER).replaceItem(house, id, key, new CosmosItemRequestOptions());
    }

    public void deleteHouse(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        db.getContainer(HousesResource.CONTAINER).deleteItem(id, key, new CosmosItemRequestOptions());
    }

    public CosmosPagedIterable<HouseDAO> getHousesByLocation(String location) {
        init();
        String query = String.format("SELECT * FROM houses WHERE houses.location=\"%s\"", location);
        return db.getContainer(HousesResource.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// RENTALS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosItemResponse<RentalDAO> updateRental(RentalDAO rental) {
        init();
        PartitionKey key = new PartitionKey(rental.getHouseId());
        return db.getContainer(RentalResource.CONTAINER).replaceItem(rental, rental.getId(), key, new CosmosItemRequestOptions());
    }

    public CosmosItemResponse<Object> deleteRental(String houseId, String id) {
        init();
        PartitionKey key = new PartitionKey(houseId);
        return db.getContainer(RentalResource.CONTAINER).deleteItem(id, key, new CosmosItemRequestOptions());
    }

    public CosmosPagedIterable<RentalDAO> getRentalById(String houseId, String id) {
        init();
        String query = "SELECT * FROM rentals WHERE rentals.houseId = \"" + houseId + "\" AND rentals.id=\"" + id + "\"";
        return db.getContainer(RentalResource.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }

    public CosmosPagedIterable<RentalDAO> getRentals(String houseID) {
        init();
        PartitionKey key = new PartitionKey(houseID);
        return db.getContainer(RentalResource.CONTAINER).readAllItems(key, RentalDAO.class); // (query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// QUESTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void replyToQuestion(QuestionDAO question) throws Exception {
        init();
        var id = question.getId();
        PartitionKey key = new PartitionKey(question.getHouseId());
        db.getContainer(QuestionResource.CONTAINER).replaceItem(question, id, key, new CosmosItemRequestOptions());
    }

    public CosmosPagedIterable<QuestionDAO> listHouseQuestions(String houseId) {
        init();
        String query = String.format("SELECT * FROM questions WHERE questions.houseId=\"%s\"", houseId);
        return db.getContainer(QuestionResource.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), QuestionDAO.class);
    }


    public void close() {
        client.close();
    }


}
