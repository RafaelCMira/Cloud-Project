package scc.srv.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.users.UsersResource;
import scc.srv.utils.Checks;
import scc.srv.houses.HousesService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QuestionResource implements QuestionService {
    private final String BAD_REQUEST = "BAD_REQUEST@Some mandatory value is empty";
    private final String NOT_FOUND = "NOT_FOUND@%s: %s does not exist";

    private final String NOT_FOUND2 = "%s: %s  %s does not exist";
    public static final String CONTAINER = "questions";
    public static final String PARTITION_KEY = "/houseId";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public Response createQuestion(QuestionDAO questionDAO) {
        // TODO: Cache

        if (Checks.badParams(questionDAO.getAskerId(), questionDAO.getHouseId(), questionDAO.getText()))
            return sendResponse(BAD_REQUEST);
        //return Response.status(Response.Status.BAD_REQUEST).entity(BAD_REQUEST).build();

        var houseRes = db.getById(questionDAO.getHouseId(), HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
        if (houseRes.isEmpty())
            return sendResponse(NOT_FOUND, "House", questionDAO.getHouseId());
        //return Response.status(Response.Status.NOT_FOUND).entity(NOT_FOUND).build();

        var userRes = db.getById(questionDAO.getAskerId(), UsersResource.CONTAINER, UsersResource.class).stream().findFirst();
        if (userRes.isEmpty())
            return sendResponse(NOT_FOUND, "User", questionDAO.getAskerId());
        //return Response.status(Response.Status.NOT_FOUND).entity("User: " + questionDAO.getAskerId() + " does not exist").build();

        questionDAO.setId(UUID.randomUUID().toString());
        var createRes = db.createItem(questionDAO, CONTAINER);
        int statusCode = createRes.getStatusCode();

        if (Checks.isStatusOk(statusCode))
            return Response.status(Response.Status.OK).entity(questionDAO.toQuestion().toString()).build();
        else
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + statusCode).build();
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
            if (Checks.isStatusOk(statusCode)) {
                jedis.set(CACHE_PREFIX + questionId, mapper.writeValueAsString(questionDAO));
                return questionDAO.toQuestion();
            } else
                throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public List<Question> listQuestions(String houseId) throws Exception {
        // TODO: ver se questions estao na cache
        //  se nao estiverem, por na chache

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            // Verify if house exists
            if (jedis.get(HousesService.HOUSE_PREFIX + houseId) == null) {
                var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
                if (houseRes.isEmpty())
                    throw new Exception("Error: 404 House Not Found");
            }

            var queryRes = db.listHouseQuestions(houseId);
            return queryRes.stream().map(QuestionDAO::toQuestion).toList();
        }
    }

    private Response sendResponse(String msg, Object... params) {
        var res = msg.split("@");
        Response.Status status = Response.Status.valueOf(res[0]);
        String message = String.format(res[1], params);
        return Response.status(status).entity(message).build();
    }

}
