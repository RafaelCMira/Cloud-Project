package scc.srv.question;

import com.azure.cosmos.CosmosException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.utils.Validations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import static scc.srv.utils.Utility.*;

public class QuestionResource extends Validations implements QuestionService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createQuestion(String houseId, QuestionDAO questionDAO) throws JsonProcessingException {
        try {
            questionDAO.setHouseId(houseId);
            questionDAO.setId(UUID.randomUUID().toString());

            checkQuestionCreation(questionDAO);

            db.create(questionDAO, CONTAINER);

            Cache.putInCache(questionDAO, QUESTION_PREFIX);

            //TODO (remover a linha comentada): acho que não é necesário fazer isto
            // porque num site real, nunca vamos ver questoes (clicamos sempre 1º na casa e depois é que vemos as questoes associadas)
            // em vez de termos este addToList, o que fiz foi: cada vez que fazemos um get da casa, colocamos a lista assincronamente no cache

            // Cache.addToListInCache(questionDAO.toQuestion(), QUESTIONS_LIST_PREFIX + houseId);

            return sendResponse(OK, questionDAO.toQuestion());

        } catch (CosmosException ex) {
            return handleCreateException(ex.getStatusCode(), ex.getMessage(), questionDAO);
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), questionDAO);
        }
    }

    @Override
    public Response replyToQuestion(Cookie session, String houseId, String questionId, QuestionDAO questionDAO) throws Exception {
        try {
            var updatedQuestion = genUpdatedQuestion(session, houseId, questionId, questionDAO);
            db.update(updatedQuestion, CONTAINER, updatedQuestion.getHouseId());

            Cache.putInCache(updatedQuestion, QUESTION_PREFIX);

            //TODO (remover a linha comentada): acho que não é necesário fazer isto
            // porque num site real, nunca vamos ver questoes (clicamos sempre 1º na casa e depois é que vemos as questoes associadas)
            // em vez de termos este addToList, o que fiz foi: cada vez que fazemos um get da casa, colocamos a lista assincronamente no cache

            // Cache.addToListInCache(questionDAO.toQuestion(), QUESTIONS_LIST_PREFIX + houseId);

            return sendResponse(OK, updatedQuestion.toQuestion());

        } catch (CosmosException ex) {
            return handleUpdateException(ex.getStatusCode(), ex.getMessage(), questionId, houseId);
        } catch (WebApplicationException ex) {
            return handleUpdateException(ex.getResponse().getStatus(), ex.getMessage(), questionId, houseId);
        }
    }

    @Override
    public Response listQuestions(String houseId) {

        if (Validations.houseExists(houseId) == null)
            return sendResponse(NOT_FOUND, HOUSE_MSG, houseId);

        try {
            //TODO (antes do pre-load das questoes): acho que não podemos ter isto assim porque pode não estar atualizado
            // Se apenas enviamos o que está na cache podemos apenas enviar a última question feita
            // e as outras que são mais antigas já sairam da cache e não vao aparecer na lista

            //TODO (depois do pre-load): assim acho que faz sentido termos isto. Caaa vez que fazemos get da casa,
            // e consequentemente fazemos um get das questoes da casa, elas vão estar na cache, logo vê sempre o mais atual
            var cacheQuestions = Cache.getListFromCache(QUESTIONS_LIST_PREFIX + houseId);

            if (!cacheQuestions.isEmpty()) {
                var questions = new ArrayList<>();
                for (String question : cacheQuestions) {
                    questions.add(mapper.readValue(question, Question.class));
                }
                return sendResponse(OK, questions);
            }

            //TODO: não devemos fazer upload da lista toda se for muito grande (10 questions por casa no max)
            var questions = db.listHouseQuestions(houseId).stream().map(QuestionDAO::toQuestion).toList();

            Cache.uploadListToCache(questions, QUESTIONS_LIST_PREFIX + houseId);

            return sendResponse(OK, questions);

        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        } catch (JsonProcessingException e) {
            return processException(500, "Error while parsing questions");
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
