package scc.srv.question;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.users.UsersResource;
import scc.srv.houses.HousesService;
import scc.srv.users.UsersService;
import scc.srv.utils.Validations;

import java.util.UUID;


import static scc.srv.utils.Utility.*;

public class QuestionResource implements QuestionService {

    public static final String CONTAINER = "questions";
    public static final String PARTITION_KEY = "/houseId";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createQuestion(String houseId, QuestionDAO questionDAO) throws JsonProcessingException {
        try {
            questionDAO.setHouseId(houseId);

            checkQuestionCreation(questionDAO);

            questionDAO.setId(UUID.randomUUID().toString());
            db.createItem(questionDAO, CONTAINER);

            Cache.putInCache(questionDAO, QUESTION_PREFIX);

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
        try {

            // Verify if house exists
            HouseDAO house;
            var cacheHouse = Cache.getFromCache(HousesService.HOUSE_PREFIX, houseId);
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

            Cache.putInCache(questionDAO, QUESTION_PREFIX);

            return sendResponse(OK, questionDAO.toQuestion());

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    @Override
    public Response listQuestions(String houseId) {
        // TODO: colocar lista de questoes na cache

        try {
            // Verify if house exists
            if (Cache.getFromCache(HousesService.HOUSE_PREFIX, houseId) == null) {
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

    private void checkQuestionCreation(QuestionDAO questionDAO) throws WebApplicationException {
        if (badParams(questionDAO.getHouseId(), questionDAO.getAskerId(), questionDAO.getText()))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        if (!Validations.houseExists(questionDAO.getHouseId()))
            throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);
        
        if (!Validations.userExists(questionDAO.getAskerId()))
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);
    }


}
