package scc.srv.question;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.House;
import scc.data.QuestionDAO;

@Path(QuestionService.PATH)

public interface QuestionService {
    String PATH = "/house";
    String HOUSE_ID = "id";
    String QUESTION = "/question";
    String QUESTION_ID = "/question";

    @POST
    @Path("/{" + HOUSE_ID + "}" + QUESTION)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createQuestion(QuestionDAO questionDAO) throws Exception;

    @PUT
    @Path("/{" + HOUSE_ID + "}" + QUESTION + "/{" + QUESTION_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    House updateQuestion(@PathParam(HOUSE_ID) String houseID, @PathParam(QUESTION_ID) String questionID, QuestionDAO questionDAO) throws Exception;

    @GET
    @Path("/{" + HOUSE_ID + "}" + QUESTION)
    @Produces(MediaType.APPLICATION_JSON)
    House listQuestions(@PathParam(HOUSE_ID) String houseID) throws Exception;
}
