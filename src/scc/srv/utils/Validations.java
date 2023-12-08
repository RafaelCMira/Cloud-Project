package scc.srv.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import scc.cache.Cache;
import scc.data.HouseDAO;
import scc.data.QuestionDAO;
import scc.data.RentalDAO;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.db.MongoDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.houses.HousesService;
import scc.srv.media.MediaResource;
import scc.srv.question.QuestionService;
import scc.srv.rentals.RentalService;
import scc.srv.users.UsersService;

import java.util.Date;
import java.util.List;

public class Validations {

    private static final CosmosDBLayer db = null;//CosmosDBLayer.getInstance();
    private static final MongoDBLayer mongoDBLayer = MongoDBLayer.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    public Validations() {
    }

    protected static boolean badParams(String... values) {
        for (var str : values)
            if (str == null || str.isBlank())
                return true;
        return false;
    }

    /**
     * Verify if house exists
     */
    protected static UserDAO userExists(String userId) {
        try {
            var userCache = Cache.getFromCache(UsersService.USER_PREFIX, userId);
            if (userCache != null)
                return mapper.readValue(userCache, UserDAO.class);

            Document dbUser = mongoDBLayer.get(userId, UsersService.COLLECTION);
            if (dbUser != null) {
                return UserDAO.fromDocument(dbUser);
            }
            
        } catch (JsonProcessingException ignore) {
        }

        return null;
    }

    /**
     * Verify if house exists
     */
    protected static HouseDAO houseExists(String houseId) {
        try {
            var cacheHouse = Cache.getFromCache(HousesService.HOUSE_PREFIX, houseId);
            if (cacheHouse != null)
                return mapper.readValue(cacheHouse, HouseDAO.class);

            Document dbHouse = mongoDBLayer.get(houseId, HousesService.COLLECTION);
            if (dbHouse != null) {
                return HouseDAO.fromDocument(dbHouse);
            }

        } catch (JsonProcessingException ignore) {
        }

        return null;
    }

    /**
     * Verify if house exists
     */
    protected static RentalDAO rentalExists(String rentalId) {
        try {
            var rentalCache = Cache.getFromCache(RentalService.RENTAL_PREFIX, rentalId);
            if (rentalCache != null)
                return mapper.readValue(rentalCache, RentalDAO.class);

            var dbRental = db.get(rentalId, RentalService.COLLECTION, RentalDAO.class).stream().findFirst();
            if (dbRental.isPresent())
                return dbRental.get();

        } catch (JsonProcessingException ignore) {
        }

        return null;
    }


    /**
     * Verify if questiom exists
     */
    protected static QuestionDAO questionExists(String questionId) {
        try {
            var questionCache = Cache.getFromCache(QuestionService.QUESTION_PREFIX, questionId);
            if (questionCache != null)
                return mapper.readValue(questionCache, QuestionDAO.class);

            var dbQuestion = db.get(questionId, QuestionService.COLLECTION, QuestionDAO.class).stream().findFirst();
            if (dbQuestion.isPresent())
                return dbQuestion.get();

        } catch (JsonProcessingException ignore) {
        }

        return null;
    }


    /**
     * Verify if media exists
     */
    protected static boolean mediaExists(List<String> mediaId) {
        MediaResource media = new MediaResource();
        return media.hasPhotos(mediaId) && mediaId != null && !mediaId.isEmpty();
    }

    /**
     * Verify if house is available
     */
    protected static boolean isAvailable(String houseId, Date start, Date end) {
        var rentals = db.getAllHouseRentals(houseId);

        for (RentalDAO rental : rentals) {
            if (!(start.after(rental.getEndDate()) || rental.getInitialDate().after(end)))
                return false;
        }

        return true;
    }


}
