package scc.srv.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.users.UsersResource;
import scc.srv.houses.HousesService;

import java.util.Optional;
import java.util.UUID;


import static scc.srv.utils.Utility.*;

public class QuestionResource implements QuestionService {

    public static final String CONTAINER = "questions";
    public static final String PARTITION_KEY = "/houseId";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createQuestion(QuestionDAO questionDAO) {
        // TODO: Cache
        if (badParams(questionDAO.getAskerId(), questionDAO.getHouseId(), questionDAO.getText()))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        // House not found
        var houseRes = db.getById(questionDAO.getHouseId(), HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
        if (houseRes.isEmpty())
            return sendResponse(NOT_FOUND, HOUSE_MSG, questionDAO.getHouseId());

        // House not found
        var userRes = db.getById(questionDAO.getAskerId(), UsersResource.CONTAINER, UsersResource.class).stream().findFirst();
        if (userRes.isEmpty())
            return sendResponse(NOT_FOUND, USER_MSG, questionDAO.getAskerId());

        questionDAO.setId(UUID.randomUUID().toString());
        var createRes = db.createItem(questionDAO, CONTAINER);
        int statusCode = createRes.getStatusCode();

        if (isStatusOk(statusCode))
            return sendResponse(OK, questionDAO.toQuestion().toString());
        else
            return sendResponse(INTERNAL_SERVER_ERROR);
    }


    @Override
    public Question replyToQuestion(String houseId, String questionId, String replierId, QuestionDAO questionDAO) throws Exception {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            // Verify if house exists
            HouseDAO house = mapper.readValue(jedis.get(HousesService.HOUSE_PREFIX + houseId), HouseDAO.class);
            if (house == null) {
                var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class);
                Optional<HouseDAO> hResult = houseRes.stream().findFirst();
                if (hResult.isEmpty())
                    throw new Exception("Error: 404 House Not Found ");
                house = hResult.get();
            }

            // Verify if user who replies is the owner
            String ownerID = house.getOwnerId();
            if (!ownerID.equals(replierId))
                throw new Exception("Error: 403 You're not the owner");

            var replyRes = db.replyToQuestion(houseId, questionId, questionDAO.getAnswer());

            int statusCode = replyRes.getStatusCode();
            if (isStatusOk(statusCode)) {
                jedis.set(CACHE_PREFIX + questionId, mapper.writeValueAsString(questionDAO));
                return questionDAO.toQuestion();
            } else
                throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public Response listQuestions(String houseId) throws Exception {
        // TODO: ver se questions estao na cache
        //  se nao estiverem, por na chache

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            // Verify if house exists
            if (jedis.get(HousesService.HOUSE_PREFIX + houseId) == null) {
                var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
                if (houseRes.isEmpty())
                    return sendResponse(NOT_FOUND, HOUSE_MSG, houseId);
            }

            var questions = db.listHouseQuestions(houseId).stream().map(QuestionDAO::toQuestion).toList();
            return sendResponse(OK, questions);
        }
    }


}
