package scc.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import scc.data.UserDAO;
import scc.srv.houses.HousesService;
import scc.srv.question.QuestionService;
import scc.srv.rentals.RentalService;
import scc.srv.users.UsersService;
import scc.srv.utils.Utility;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;


public class MongoDBLayer {

    public static final String CONNECTION_STRING = "mongodb://sccapp-mongodb-59243:27017";
    public static final String DATABASE_NAME = "myMongoDB";

    private final String ID = "id";

    private static MongoDBLayer instance;
    private final MongoClient mongoClient;
    private MongoDatabase database;

    private final ObjectMapper mapper = new ObjectMapper();

    private MongoDBLayer(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public static synchronized MongoDBLayer getInstance() {
        if (instance != null)
            return instance;

        MongoClient mongoClient = new MongoClient(new MongoClientURI(CONNECTION_STRING));
        instance = new MongoDBLayer(mongoClient);
        return instance;
    }

    private synchronized void init() {
        if (database != null)
            return;
        database = mongoClient.getDatabase(DATABASE_NAME);
    }

    public synchronized void initializeCollections() {
        init();
        createCollectionIfNotExists(UsersService.COLLECTION);
        database.getCollection(UsersService.COLLECTION).createIndex(Indexes.ascending(ID));

        createCollectionIfNotExists(HousesService.COLLECTION);
        database.getCollection(HousesService.COLLECTION).createIndex(Indexes.ascending(ID));
        database.getCollection(HousesService.COLLECTION).createIndex(Indexes.ascending("location"));

        createCollectionIfNotExists(RentalService.COLLECTION);
        database.getCollection(RentalService.COLLECTION).createIndex(Indexes.ascending("houseId"));

        createCollectionIfNotExists(QuestionService.COLLECTION);
        database.getCollection(QuestionService.COLLECTION).createIndex(Indexes.ascending("houseId"));
    }

    private void createCollectionIfNotExists(String collectionName) {
        for (String existingCollection : database.listCollectionNames()) {
            if (existingCollection.equalsIgnoreCase(collectionName)) {
                return;
            }
        }
        database.createCollection(collectionName);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// GENERICS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void create(Object item, String collectionName) {
        init();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document document = Document.parse(Utility.itemToJsonString(item));
        collection.insertOne(document);
    }

    public Document get(String id, String collectionName) {
        init();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        return collection.find(Filters.eq(ID, id)).first();
    }

    public void delete(String id, String collectionName) {
        init();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteOne(Filters.eq(ID, id));
    }

    public void update(String id, Object updatedItem, String collectionName) {
        init();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document updatedDocument = Document.parse(Utility.itemToJsonString(updatedItem));
        collection.replaceOne(Filters.eq(ID, id), updatedDocument);
    }


    public FindIterable<Document> getAll(String collectionName) {
        init();
        return database.getCollection(collectionName).find();
    }


}
