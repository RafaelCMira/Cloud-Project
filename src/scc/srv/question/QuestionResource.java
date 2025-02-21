package scc.srv.question;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.MongoDBLayer;
import scc.srv.houses.HousesService;
import scc.srv.utils.Validations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import static scc.srv.utils.Utility.*;

public class QuestionResource extends Validations implements QuestionService {

    private final MongoDBLayer db = MongoDBLayer.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createQuestion(Cookie session, String houseId, QuestionDAO questionDAO) throws Exception {
        try {
            questionDAO.setHouseId(houseId);
            questionDAO.setId(UUID.randomUUID().toString());

            checkQuestionCreation(session, questionDAO);

            db.create(questionDAO, COLLECTION);

            Cache.putInCache(questionDAO, QUESTION_PREFIX);

            return sendResponse(OK, questionDAO.toQuestion());

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), questionDAO);
        }
    }

    @Override
    public Response replyToQuestion(Cookie session, String houseId, String questionId, QuestionDAO questionDAO) throws Exception {
        try {
            var updatedQuestion = genUpdatedQuestion(session, houseId, questionId, questionDAO);
            db.update(updatedQuestion.getId(), updatedQuestion, COLLECTION);

            Cache.putInCache(updatedQuestion, QUESTION_PREFIX);

            return sendResponse(OK, updatedQuestion.toQuestion());

        } catch (MongoException ex) {
            return Response.status(500).entity(ex.getMessage()).build();
        } catch (WebApplicationException ex) {
            return handleUpdateException(ex.getResponse().getStatus(), ex.getMessage(), questionId, houseId);
        }
    }

    @Override
    public Response listHouseQuestions(String houseId, int offset) {
        var house = Validations.houseExists(houseId);
        if (house == null)
            return sendResponse(NOT_FOUND, HOUSE_MSG, houseId);

        try {
            List<Question> questions = new ArrayList<>();

            String key = String.format(QUESTIONS_LIST_PREFIX, houseId, offset);
            var cacheQuestions = Cache.getListFromCache(key);
            if (!cacheQuestions.isEmpty()) {
                for (String question : cacheQuestions) {
                    questions.add(mapper.readValue(question, Question.class));
                }
                return sendResponse(OK, questions);
            }

            questions = db.getHouseQuestions(houseId, offset);

            Cache.putListInCache(questions, key);
            Cache.putInCache(house, HousesService.HOUSE_PREFIX + houseId);

            return sendResponse(OK, questions);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        } catch (JsonProcessingException e) {
            return processException(500, "Error while parsing questions");
        }
    }

    private void checkQuestionCreation(Cookie session, QuestionDAO questionDAO) throws Exception {
        if (Validations.badParams(questionDAO.getHouseId(), questionDAO.getAskerId(), questionDAO.getText()))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        var checkCookies = checkUserSession(session, questionDAO.getAskerId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        if (Validations.houseExists(questionDAO.getHouseId()) == null)
            throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);

        if (Validations.userExists(questionDAO.getAskerId()) == null)
            throw new WebApplicationException(USER_MSG, Response.Status.NOT_FOUND);
    }

    private QuestionDAO genUpdatedQuestion(Cookie session, String houseId, String questionId, QuestionDAO questionDAO) throws Exception {
        String answer = questionDAO.getAnswer();

        if (Validations.badParams(answer))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

        var house = Validations.houseExists(houseId);
        if (house == null)
            throw new WebApplicationException(HOUSE_MSG, Response.Status.NOT_FOUND);

        var checkCookies = checkUserSession(session, house.getOwnerId());
        if (checkCookies.getStatus() != Response.Status.OK.getStatusCode())
            throw new WebApplicationException(checkCookies.getEntity().toString(), Response.Status.UNAUTHORIZED);

        var question = Validations.questionExists(questionId);
        if (question == null)
            throw new WebApplicationException(QUESTION_MSG, Response.Status.NOT_FOUND);

        if (!question.getAnswer().isBlank())
            throw new WebApplicationException(QUESTION_MSG, Response.Status.CONFLICT);

        question.setAnswer(answer);

        return question;
    }

    private Response handleCreateException(int statusCode, String msg, QuestionDAO questionDAO) {
        if (msg.contains(HOUSE_MSG))
            return processException(statusCode, HOUSE_MSG, questionDAO.getHouseId());
        else if (msg.contains(USER_MSG))
            return processException(statusCode, USER_MSG, questionDAO.getAskerId());
        else
            return processException(statusCode, msg);
    }

    private Response handleUpdateException(int statusCode, String msg, String id, String houseId) {
        if (statusCode == 409)
            return Response.status(Response.Status.CONFLICT).entity(String.format(QUESTION_ALREADY_ANSWERED, id)).build();
        if (msg.contains(QUESTION_MSG))
            return processException(statusCode, QUESTION_MSG, id);
        else if (msg.contains(HOUSE_MSG))
            return processException(statusCode, HOUSE_MSG, houseId);
        else
            return processException(statusCode, msg, id);
    }

}
