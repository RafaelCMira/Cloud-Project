package scc.srv.question;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.data.House;
import scc.data.Question;
import scc.data.QuestionDAO;

import java.util.List;

@Path(QuestionService.PATH)
public interface QuestionService {
    String PATH = "/house";
    String HOUSE_ID = "id";
    String QUESTION = "/question";
    String QUESTION_ID = "questionId";
    String REPLIER_ID = "replierId";
    String CACHE_PREFIX = "question:";


    @POST
    @Path("/{" + HOUSE_ID + "}" + QUESTION)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createQuestion(QuestionDAO questionDAO) throws Exception;

    @PUT
    @Path("/{" + HOUSE_ID + "}" + QUESTION + "/{" + QUESTION_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Question replyToQuestion(@PathParam(HOUSE_ID) String houseId,
                             @PathParam(QUESTION_ID) String questionId,
                             @QueryParam(REPLIER_ID) String replierId,
                             QuestionDAO questionDAO)
            throws Exception;

    @GET
    @Path("/{" + HOUSE_ID + "}" + QUESTION)
    @Produces(MediaType.APPLICATION_JSON)
    List<Question> listQuestions(@PathParam(HOUSE_ID) String houseId) throws Exception;
}
