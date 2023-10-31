package scc.srv.question;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import scc.srv.utils.Cache;

import java.util.Optional;
import java.util.UUID;


import static scc.srv.utils.Utility.*;

public class QuestionResource implements QuestionService {

    public static final String CONTAINER = "questions";
    public static final String PARTITION_KEY = "/houseId";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createQuestion(String houseId, QuestionDAO questionDAO) throws JsonProcessingException {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            questionDAO.setHouseId(houseId);

            checkQuestionCreation(questionDAO, jedis);

            questionDAO.setId(UUID.randomUUID().toString());
            db.createItem(questionDAO, CONTAINER);

            Cache.putInCache(questionDAO, QUESTION_PREFIX, jedis);

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
    public Response replyToQuestion(String houseId, String questionId, String replierId, QuestionDAO questionDAO) throws Exception {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            // Verify if house exists
            HouseDAO house = null;
            var cacheHouse = jedis.get(HousesService.HOUSE_PREFIX + houseId);
            if (cacheHouse != null)
                house = mapper.readValue(cacheHouse, HouseDAO.class);
            else {
                var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
                if (houseRes.isEmpty())
                    return sendResponse(NOT_FOUND, HOUSE_MSG);
                house = houseRes.get();
            }

            // Verify if user who replies is the owner
            if (!house.getOwnerId().equals(replierId))
                return sendResponse(FORBIDDEN, NOT_THE_OWNER);

            //todo: usar cookies para verificar se ele existe

            db.replyToQuestion(houseId, questionId, questionDAO.getAnswer());

            jedis.set(QUESTION_PREFIX + questionId, mapper.writeValueAsString(questionDAO));

            return sendResponse(OK, questionDAO.toQuestion());

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    @Override
    public Response listQuestions(String houseId) {
        // TODO: colocar lista de questoes na cache

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            // Verify if house exists
            if (jedis.get(HousesService.HOUSE_PREFIX + houseId) == null) {
                var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
                if (houseRes.isEmpty())
                    return sendResponse(NOT_FOUND, HOUSE_MSG, houseId);
            }

            var questions = db.listHouseQuestions(houseId).stream().map(QuestionDAO::toQuestion).toList();
            return sendResponse(OK, questions);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
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
