package scc.srv.question;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.users.UsersResource;
import scc.srv.houses.HousesService;
import scc.srv.users.UsersService;

import java.util.Optional;
import java.util.UUID;


import static scc.srv.utils.Utility.*;

public class QuestionResource implements QuestionService {

    public static final String CONTAINER = "questions";
    public static final String PARTITION_KEY = "/houseId";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createQuestion(String houseId, QuestionDAO questionDAO) {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            questionDAO.setHouseId(houseId);

            checkQuestionCreation(questionDAO, jedis);

            questionDAO.setId(UUID.randomUUID().toString());
            db.createItem(questionDAO, CONTAINER);

            return sendResponse(OK, questionDAO.toQuestion().toString());

        } catch (CosmosException ex) {
            return handleCreateException(ex.getStatusCode(), ex.getMessage(), questionDAO);
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), questionDAO);
        }
    }

    private Response handleCreateException(int statusCode, String msg, QuestionDAO questionDAO) {
        if (msg.contains(HOUSE_MSG))
            return processException(statusCode, HOUSE_MSG, questionDAO.getHouseId());
        else if (msg.contains(USER_MSG))
            return processException(statusCode, USER_MSG, questionDAO.getAskerId());
        else
            return processException(statusCode, msg);
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

    private void checkQuestionCreation(QuestionDAO questionDAO, Jedis jedis) throws WebApplicationException {
        if (badParams(questionDAO.getHouseId(), questionDAO.getAskerId(), questionDAO.getText()))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        // Check if house doesn't exist on Cache
        if (jedis.get(HousesService.HOUSE_PREFIX + questionDAO.getHouseId()) == null) {
            // Check if house exists on DB
            var house = db.getById(questionDAO.getHouseId(), CONTAINER, HouseDAO.class).stream().findFirst();
            if (house.isEmpty())
                throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);
        }

        // Check if user doesn't exist on Cache
        if (jedis.get(UsersService.USER_PREFIX + questionDAO.getAskerId()) == null) {
            // Check if user exists on DB
            var user = db.getById(questionDAO.getAskerId(), UsersResource.CONTAINER, UserDAO.class).stream().findFirst();
            if (user.isEmpty())
                throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);
        }
    }


}
