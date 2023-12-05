package scc.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jdk.jshell.execution.Util;
import org.bson.Document;
import scc.srv.houses.HousesService;
import scc.srv.question.QuestionService;
import scc.srv.rentals.RentalService;
import scc.srv.users.UsersService;
import scc.srv.utils.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MongoDBLayer {

    public static final String CONNECTION_STRING = "mongodb://sccapp-mongodb-59243:27017";
    public static final String DATABASE_NAME = "myMongoDB";

    private final String _ID = "_id";

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
        createCollectionIfNotExists(HousesService.COLLECTION);
        createCollectionIfNotExists(RentalService.COLLECTION);
        createCollectionIfNotExists(QuestionService.COLLECTION);
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
        return collection.find(Filters.eq(_ID, id)).first();
    }

    public void delete(String id, String collectionName) {
        init();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.deleteOne(new Document(_ID, id));
    }

    public <T> List<T> getAll(String collectionName, Class<T> c) {
        init();
        FindIterable<Document> documents = database.getCollection(collectionName).find();

        List<T> resultList = new ArrayList<>();

        for (Document doc : documents) {
            T item = Utility.mapDocumentToObject(doc, c);
            resultList.add(item);
        }

        return resultList;
    }


}
