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
    String CONTAINER = "questions";
    String QUESTION_PREFIX = "q:";
    String CACHE_LIST = "list:q:";

    String HOUSE_ID = "id";
    String QUESTION = "/question";
    String QUESTION_ID = "questionId";
    String REPLIER_ID = "replierId";


    @POST
    @Path("/{" + HOUSE_ID + "}" + QUESTION)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createQuestion(@PathParam(HOUSE_ID) String houseId, QuestionDAO questionDAO) throws Exception;

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
    Response listQuestions(@PathParam(HOUSE_ID) String houseId) throws Exception;
}
