package scc.db;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;

import scc.data.*;
import scc.utils.props.AzureProperties;

public class CosmosDBLayer {

    public static final String USERS_CONTAINER = "users";
    public static final String HOUSES_CONTAINER = "houses";
    public static final String RENTALS_CONTAINER = "rentals";
    public static final String QUESTIONS_CONTAINER = "questions";


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
    ////////////////////////////// USERS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosItemResponse<Object> createItem(Object item, String container) {
        init();
        return db.getContainer(container).createItem(item);
    }

    public CosmosItemResponse<Object> deleteById(String id, String container, String partitionKey) throws Exception {
        init();
        PartitionKey key = new PartitionKey(partitionKey);
        try {
            return db.getContainer(container).deleteItem(id, key, new CosmosItemRequestOptions());
        } catch (Exception e) {
            throw new Exception("Error: " + e.getCause());
        }
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
        // return db.getContainer(container).readAllItems(key, c);
    }

    /*public CosmosItemResponse<Object> delUser(UserDAO user) {
        init();
        return container.deleteItem(user, new CosmosItemRequestOptions());
    }*/

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

/*    public CosmosPagedIterable<UserDAO> listUsers() {
        init();
        String query = "SELECT * FROM users";
        return db.getContainer(USERS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), UserDAO.class);
    }*/

    // TODO: Perguntar se no listar casas de um user podemos só listar o id da casa OU se temos de listar o JSON das casas
    public CosmosPagedIterable<String> listUserHouses(String id) {
        init();
        String query = String.format("SELECT housesIds FROM users WHERE users.id=\"%s\"", id);
        return db.getContainer(USERS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), String.class);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// HOUSES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public CosmosItemResponse<HouseDAO> updateHouseById(String id, HouseDAO house) {
        init();
        PartitionKey key = new PartitionKey(id);
        return db.getContainer(HOUSES_CONTAINER).replaceItem(house, id, key, new CosmosItemRequestOptions());
    }

    public CosmosPagedIterable<HouseDAO> getHousesByLocation(String location) {
        init();
        String query = String.format("SELECT * FROM houses WHERE houses.location=\"%s\"", location);
        return db.getContainer(HOUSES_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// RENTALS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosPagedIterable<RentalDAO> getRentalById(String houseID, String id) {
        init();
        String query = "SELECT * FROM rentals WHERE rentals.houseID = \"" + houseID + "\" AND rentals.id=\"" + id + "\"";
        return db.getContainer(RENTALS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }

    public CosmosPagedIterable<RentalDAO> getRentals(String houseID) {
        init();
        PartitionKey key = new PartitionKey(houseID);
        return db.getContainer(RENTALS_CONTAINER).readAllItems(key, RentalDAO.class); // (query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }

    public CosmosItemResponse<RentalDAO> updateRentalById(String houseId, RentalDAO rentalDAO) {
        init();
        PartitionKey key = new PartitionKey(houseId);
        return db.getContainer(RENTALS_CONTAINER).replaceItem(rentalDAO, houseId, key, new CosmosItemRequestOptions());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// QUESTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosItemResponse<QuestionDAO> replyToQuestion(String houseID, String questionID, String answer) throws Exception {
        init();
        PartitionKey key = new PartitionKey(houseID);

        // Verify if question exists
        // query to retrieve the item you want to update.
        var question = getQuestionByID(houseID, questionID).stream().findFirst();

        // Check if the query returned any results.
        if (question.isEmpty())
            throw new Exception("Error: 404 Question Not Found");

        // Verify if it has already been answered
        QuestionDAO existingQuestion = question.get();
        if (existingQuestion.getAnswer().isEmpty())
            throw new Exception("Error: 403 Question Already Answered");

        // Update the answer
        existingQuestion.setAnswer(answer);
        return db.getContainer(QUESTIONS_CONTAINER).replaceItem(existingQuestion, questionID, key, new CosmosItemRequestOptions());
    }


    public CosmosPagedIterable<QuestionDAO> getQuestionByID(String houseID, String questionID) {
        init();
        String query = String.format("SELECT * FROM questions WHERE questions.questionID = '%s' AND questions.houseID = '%s'", houseID, questionID);
        return db.getContainer(QUESTIONS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), QuestionDAO.class);
    }

    public CosmosPagedIterable<QuestionDAO> listHouseQuestions(String houseId) {
        init();
        String query = String.format("SELECT * FROM questions WHERE questions.houseID=\"%s\"", houseId);
        return db.getContainer(QUESTIONS_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), QuestionDAO.class);
    }


    public void close() {
        client.close();
    }


}
