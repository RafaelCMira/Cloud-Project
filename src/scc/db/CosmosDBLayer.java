package scc.db;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.*;
import scc.srv.houses.HousesService;
import scc.srv.question.QuestionService;
import scc.srv.rentals.RentalService;
import scc.srv.utils.HasId;
import scc.utils.props.AzureProperties;

public class CosmosDBLayer {

    private static final String CONNECTION_URL = System.getenv(AzureProperties.COSMOSDB_URL);
    private static final String DB_KEY = System.getenv(AzureProperties.COSMOSDB_KEY);
    private static final String DB_NAME = System.getenv(AzureProperties.COSMOSDB_DATABASE);


    private static final String GET_ALL_LIMIT = "30";

    private static final String USER_HOUSES_LIMIT = "25";

    private static final String HOUSES_LIMIT = "25";

    private static final String QUESTIONS_LIMIT = "5";

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
                .directMode()
                // .gatewayMode() // replace by .directMode() for better performance
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

    public void create(Object item, String container) {
        init();
        db.getContainer(container).createItem(item);
    }

    public void delete(String id, String container, String partitionKey) {
        init();
        PartitionKey key = new PartitionKey(partitionKey);
        db.getContainer(container).deleteItem(id, key, new CosmosItemRequestOptions());
    }

    public <T extends HasId> void update(T item, String container, String partitionKey) {
        init();
        var id = item.getId();
        PartitionKey key = new PartitionKey(partitionKey);
        db.getContainer(container).replaceItem(item, id, key, new CosmosItemRequestOptions());
    }

    public <T> CosmosPagedIterable<T> get(String id, String container, Class<T> c) {
        init();
        String query = String.format("SELECT * FROM %s WHERE %s.id=\"%s\"", container, container, id);
        return db.getContainer(container).queryItems(query, new CosmosQueryRequestOptions(), c);
    }

    public <T> CosmosPagedIterable<T> getAll(String container, Class<T> c) {
        init();
        String query = String.format("SELECT * FROM %s", container);
        return db.getContainer(container).queryItems(query, new CosmosQueryRequestOptions(), c);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// USERS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosPagedIterable<HouseDAO> listUserHouses(String id, String offset) {
        init();
        String query = String.format("SELECT * FROM houses WHERE houses.ownerId=\"%s\" OFFSET %s LIMIT %s", id, offset, USER_HOUSES_LIMIT);
        return db.getContainer(HousesService.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// HOUSES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosPagedIterable<HouseDAO> getHousesByLocation(String location, String offset) {
        init();
        String query = String.format("SELECT * FROM houses WHERE houses.location=\"%s\" OFFSET %s LIMIT %s", location, offset, HOUSES_LIMIT);
        return db.getContainer(HousesService.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
    }

    // Query usada para colocar deleted user nas houses dele quando é eliminado
    public CosmosPagedIterable<HouseDAO> getUserHouses(String ownerId) {
        init();
        String query = String.format("SELECT * FROM houses WHERE houses.ownerId=\"%s\"", ownerId);
        return db.getContainer(HousesService.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
    }


    // get houses in with discount
    public CosmosPagedIterable<HouseDAO> getHousesWithDiscount(String offset) {
        init();
        String query = String.format("SELECT * FROM houses WHERE houses.discount > 0 OFFSET %s LIMIT %s", offset, HOUSES_LIMIT);
        return db.getContainer(HousesService.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), HouseDAO.class);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// RENTALS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO: tentar fazer query que devolve apenas os rentals que estão entre estas datas (em vez de fazer o filtro no server, faz-se na DB)
    public CosmosPagedIterable<RentalDAO> getAllHouseRentals(String houseId) {
        init();
        var key = new PartitionKey(houseId);
        return db.getContainer(RentalService.CONTAINER).readAllItems(key, RentalDAO.class);
    }

    public CosmosPagedIterable<RentalDAO> getHouseRentals(String houseId, String offset) {
        init();
        String query = String.format("SELECT * FROM rentals WHERE rentals.houseId=\"%s\" OFFSET %s LIMIT %s", houseId, offset, HOUSES_LIMIT);
        return db.getContainer(RentalService.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }

    // Query usada para colocar deleted user nos rentals dele quando é eliminado
    public CosmosPagedIterable<RentalDAO> getAllUserRentals(String userId) {
        init();
        String query = String.format("SELECT * FROM rentals WHERE rentals.userId=\"%s\"", userId);
        return db.getContainer(RentalService.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }

    public CosmosPagedIterable<RentalDAO> getUserRentals(String userId, String offset) {
        init();
        String query = String.format("SELECT * FROM rentals WHERE rentals.userId=\"%s\" OFFSET %s LIMIT %s", userId, offset, HOUSES_LIMIT);
        return db.getContainer(RentalService.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), RentalDAO.class);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// QUESTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public CosmosPagedIterable<QuestionDAO> getHouseQuestions(String houseId, String offset) {
        init();
        String query = String.format("SELECT * FROM questions WHERE questions.houseId=\"%s\" OFFSET %s LIMIT %s", houseId, offset, QUESTIONS_LIMIT);
        return db.getContainer(QuestionService.CONTAINER).queryItems(query, new CosmosQueryRequestOptions(), QuestionDAO.class);
    }

    public void close() {
        client.close();
    }

}
