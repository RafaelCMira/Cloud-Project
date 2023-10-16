package scc.db;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;

import scc.data.*;
import scc.utils.props.AzureProperties;

public class CosmosDBLayer {

    public static final String USERS_CONTAINER = "users";
    public static final String USERS_PARTITION_KEY = "id";

    public static final String HOUSES_CONTAINER = "houses";
    public static final String HOUSES_PARTITION_KEY = "/houseID";

    public static final String RENTALS_CONTAINER = "rentals";
    public static final String RENTALS_PARTITION_KEY = "/houseID";

    public static final String QUESTIONS_CONTAINER = "questions";
    public static final String QUESTIONS_PARTITION_KEY = "/houseID";


    //private static final String CONNECTION_URL = System.getenv(AzureProperties.COSMOSDB_URL);
    private static final String CONNECTION_URL = "https://scc24account60700.documents.azure.com:443/";

    //private static final String DB_KEY = System.getenv(AzureProperties.COSMOSDB_KEY);
    private static final String DB_KEY = "Km0YTZA2am4j5XUC26S9PxaFw0kbK8eic2mg4KagndPUzCiZ01slWV1LyXtpSBvyM5qlUVg0IFxkACDbgoEXeQ==";

    //private static final String DB_NAME = System.getenv(AzureProperties.COSMOSDB_DATABASE);
    private static final String DB_NAME = "scc24db60700";


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
        String query = String.format("SELECT * FROM users WHERE users.id=\"%s\"", id);
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

    public CosmosPagedIterable<UserDAO> listUsers() {
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
        String query = String.format("SELECT * FROM houses WHERE houses.id=\"%s\"", id);
        return db.getContainer(HOUSES_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
    }

    public CosmosItemResponse<HouseDAO> updateHouseById(String id, HouseDAO house) {
        init();
        PartitionKey key = new PartitionKey(id);
        return db.getContainer(HOUSES_CONTAINER).replaceItem(house, id, key, new CosmosItemRequestOptions());
    }

    public CosmosPagedIterable<HouseDAO> getHousesLocation(String location) {
        init();
        String query = String.format("SELECT * FROM houses WHERE houses.location=\"%s\"", location);
        return db.getContainer(HOUSES_CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// QUESTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosItemResponse<QuestionDAO> createQuestion(QuestionDAO questionDAO) {
        init();
        return db.getContainer(QUESTIONS_CONTAINER).createItem(questionDAO);
    }

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
