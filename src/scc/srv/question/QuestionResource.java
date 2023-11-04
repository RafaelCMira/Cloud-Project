package scc.srv.question;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.utils.Validations;

import java.util.UUID;


import static scc.srv.utils.Utility.*;

public class QuestionResource extends Validations implements QuestionService {


    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public Response createQuestion(String houseId, QuestionDAO questionDAO) throws JsonProcessingException {
        try {
            questionDAO.setHouseId(houseId);

            checkQuestionCreation(questionDAO);

            questionDAO.setId(UUID.randomUUID().toString());
            db.createItem(questionDAO, CONTAINER);

            Cache.putInCache(questionDAO, QUESTION_PREFIX);

            return sendResponse(OK, questionDAO.toQuestion());

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
    public Response replyToQuestion(Cookie session, String houseId, String questionId, QuestionDAO questionDAO) throws Exception {

        try {
            var updatedQuestion = genUpdatedQuestion(session, houseId, questionId, questionDAO);
            db.replyToQuestion(updatedQuestion);

            Cache.putInCache(updatedQuestion, QUESTION_PREFIX);

            return sendResponse(OK, updatedQuestion.toQuestion());

        } catch (CosmosException ex) {
            return handleUpdateException(ex.getStatusCode(), ex.getMessage(), questionId);
        } catch (WebApplicationException ex) {
            return handleUpdateException(ex.getResponse().getStatus(), ex.getMessage(), questionId);
        }
    }

    private Response handleUpdateException(int statusCode, String msg, String id) {
        if (statusCode == 409)
            return Response.status(Response.Status.CONFLICT).entity(String.format("Question %s already answered", id)).build();
        if (msg.contains(QUESTION_MSG))
            return processException(statusCode, QUESTION_MSG, id);
        else if (msg.contains(HOUSE_MSG))
            return processException(statusCode, HOUSE_MSG, id);
        else
            return processException(statusCode, msg, id);
    }

    @Override
    public Response listQuestions(String houseId) {
        // TODO: colocar lista de questoes na cache

        try {
            if (Validations.houseExists(houseId) == null)
                return sendResponse(NOT_FOUND, HOUSE_MSG, houseId);

            var questions = db.listHouseQuestions(houseId).stream().map(QuestionDAO::toQuestion).toList();
            return sendResponse(OK, questions);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    private void checkQuestionCreation(QuestionDAO questionDAO) throws WebApplicationException {
        if (Validations.badParams(questionDAO.getHouseId(), questionDAO.getAskerId(), questionDAO.getText()))
            throw new WebApplicationException(BAD_REQUEST_MSG, Response.Status.BAD_REQUEST);

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

        // Se a questao já foi respondida
        if (!question.getAnswer().isBlank())
            throw new WebApplicationException(QUESTION_MSG, Response.Status.CONFLICT);

        question.setAnswer(answer);

        return question;

    }


}
