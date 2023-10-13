package scc.srv.question;

import scc.data.House;
import scc.data.HouseDAO;
import scc.data.QuestionDAO;
import scc.db.CosmosDBLayer;
import scc.srv.Checks;

import java.util.Optional;

public class QuestionResource implements QuestionService {

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    @Override
    public String createQuestion(QuestionDAO questionDAO) throws Exception {
        if (Checks.badParams(questionDAO.getUserID(), questionDAO.getHouseID(), questionDAO.getText()))
            throw new Exception("Error: 400 Bad Request");

        var res = db.getHouseById(questionDAO.getHouseID());
        Optional<HouseDAO> result = res.stream().findFirst();
        if (result.isEmpty())
            throw new Exception("Error: 404");

        var userRes = db.getUserById(questionDAO.getUserID());
        var userResult = userRes.stream().findFirst();
        if (userResult.isEmpty())
            throw new Exception("Error: 404");

        return null;
    }

    @Override
    public House updateQuestion(String houseID, String questionID, QuestionDAO questionDAO) throws Exception {
        return null;
    }

    @Override
    public House listQuestions(String houseID) throws Exception {
        return null;
    }
}
