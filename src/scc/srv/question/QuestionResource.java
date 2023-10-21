package scc.srv.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.Checks;
import scc.srv.houses.HousesService;

import javax.naming.LinkException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QuestionResource implements QuestionService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String createQuestion(QuestionDAO questionDAO) throws Exception {
        //TODO: chache

        if (Checks.badParams(questionDAO.getUserID(), questionDAO.getHouseID(), questionDAO.getText()))
            throw new Exception("Error: 400 Bad Request");

        // Verify if house exists
        var houseRes = db.getHouseById(questionDAO.getHouseID());
        Optional<HouseDAO> result = houseRes.stream().findFirst();
        if (result.isEmpty())
            throw new Exception("Error: 404 House Not Found ");

        // Verify if user of makes the question exists
        var userRes = db.getUserById(questionDAO.getUserID());
        var userResult = userRes.stream().findFirst();
        if (userResult.isEmpty())
            throw new Exception("Error: 404 User Not Found");

        var createRes = db.createQuestion(questionDAO);
        int statusCode = createRes.getStatusCode();

        if (Checks.isStatusOk(statusCode))
            return questionDAO.toQuestion().toString();
        else
            throw new Exception("Error: " + statusCode);
    }

    @Override
    public Question replyToQuestion(String houseID, String questionID, String replierID, QuestionDAO questionDAO) throws Exception {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            // Verify if house exists
            HouseDAO house = mapper.readValue(jedis.get(HousesService.CACHE_PREFIX + houseID), HouseDAO.class);
            if (house == null) {
                var houseRes = db.getHouseById(houseID);
                Optional<HouseDAO> hResult = houseRes.stream().findFirst();
                if (hResult.isEmpty())
                    throw new Exception("Error: 404 House Not Found ");
                house = hResult.get();
            }

            // Verify if user who replies is the owner
            String ownerID = house.getOwnerID();
            if (!ownerID.equals(replierID))
                throw new Exception("Error: 403 You're not the owner");

            var replyRes = db.replyToQuestion(houseID, questionID, questionDAO.getAnswer());

            int statusCode = replyRes.getStatusCode();
            if (Checks.isStatusOk(statusCode)) {
                jedis.set(CACHE_PREFIX + questionID, mapper.writeValueAsString(questionDAO));
                return questionDAO.toQuestion();
            } else
                throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public List<Question> listQuestions(String houseID) throws Exception {
        // TODO: ver se questions estao na cache
        //  se nao estiverem, por na chache

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            // Verify if house exists
            if (jedis.get(HousesService.CACHE_PREFIX + houseID) == null) {
                var houseRes = db.getHouseById(houseID);
                Optional<HouseDAO> result = houseRes.stream().findFirst();
                if (result.isEmpty())
                    throw new Exception("Error: 404 House Not Found");
            }

            var queryRes = db.listHouseQuestions(houseID);
            return queryRes.stream()
                    .map(QuestionDAO::toQuestion)
                    .toList();
        }
    }
}
