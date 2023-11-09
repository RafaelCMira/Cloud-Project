package scc.db;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.HouseDAO;
import scc.utils.HasId;

public class CosmosDBLayer {

    static final String HOUSES_COLLECTION = "houses";
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


    public void close() {
        client.close();
    }


}
