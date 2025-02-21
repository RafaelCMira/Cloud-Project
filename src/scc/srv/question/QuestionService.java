package scc.srv.question;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.data.QuestionDAO;
import scc.srv.authentication.Session;

@Path(QuestionService.PATH)
public interface QuestionService {
    String PATH = "/house";


    String PARTITION_KEY = "/houseId";
    String COLLECTION = "questions";
    String QUESTION_PREFIX = "q:";
    String QUESTIONS_LIST_PREFIX = "q:house:%s-off:%s";

    String HOUSE_ID = "id";
    String QUESTION = "/question";
    String QUESTION_ID = "questionId";


    String OFFSET = "offset";


    @POST
    @Path("/{" + HOUSE_ID + "}" + QUESTION)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createQuestion(@CookieParam(Session.SESSION) Cookie session, @PathParam(HOUSE_ID) String houseId, QuestionDAO questionDAO) throws Exception;

    @PUT
    @Path("/{" + HOUSE_ID + "}" + QUESTION + "/{" + QUESTION_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response replyToQuestion(@CookieParam(Session.SESSION) Cookie session,
                             @PathParam(HOUSE_ID) String houseId,
                             @PathParam(QUESTION_ID) String questionId,
                             QuestionDAO questionDAO)
            throws Exception;

    @GET
    @Path("/{" + HOUSE_ID + "}" + QUESTION)
    @Produces(MediaType.APPLICATION_JSON)
    Response listHouseQuestions(@PathParam(HOUSE_ID) String houseId, @QueryParam(OFFSET) int offset) throws Exception;
}
