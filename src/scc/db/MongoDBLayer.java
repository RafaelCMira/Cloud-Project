package scc.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import scc.data.UserDAO;
import scc.srv.houses.HousesService;
import scc.srv.question.QuestionService;
import scc.srv.rentals.RentalService;
import scc.srv.users.UsersService;
import scc.srv.utils.Utility;

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

        System.out.println("JSON Document before insertion: " + document.toJson());
        collection.insertOne(document);
        System.out.println("Document inserted successfully.");
    }



   /* public <T> T get(String id, String collectionName, Class<T> c) {
        init();
        MongoCollection<T> collection = database.getCollection(collectionName, c);
        return collection.find(Filters.eq(_ID, id)).first();
    }

    public <T> void delete(String id, String collectionName, Class<T> c) {
        init();
        MongoCollection<T> collection = database.getCollection(collectionName, c);
        collection.deleteOne(Filters.eq(_ID, id));
    }

    public <T> FindIterable<T> getAll(String collectionName, Class<T> C) {
        init();
        return database.getCollection(collectionName, C).find();
    }*/

    public Document get(String id, String collectionName) {
        init();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        return collection.find(Filters.eq("id", id)).first();
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
