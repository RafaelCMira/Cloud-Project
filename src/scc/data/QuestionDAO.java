package scc.data;

import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonId;
import scc.srv.utils.HasId;

public class QuestionDAO implements HasId {

    @BsonId
    private String id;
    private String askerId;
    private String houseId;
    private String text;
    private String answer;

    public QuestionDAO() {
    }

    public QuestionDAO(Question q) {
        this(q.getId(), q.getAskerId(), q.getHouseId(), q.getText(), q.getAnswer());
    }

    public QuestionDAO(String id, String askerId, String houseId, String text, String answer) {
        super();
        this.id = id;
        this.askerId = askerId;
        this.houseId = houseId;
        this.text = text;
        this.answer = answer;
    }

    public static QuestionDAO fromDocument(Document document) {
        QuestionDAO questionDAO = new QuestionDAO();
        questionDAO.setId(document.getString("id"));
        questionDAO.setAskerId(document.getString("askerId"));
        questionDAO.setHouseId(document.getString("houseId"));
        questionDAO.setText(document.getString("text"));
        questionDAO.setAnswer(document.getString("answer"));
        return questionDAO;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAskerId() {
        return askerId;
    }

    public void setAskerId(String askerId) {
        this.askerId = askerId;
    }

    public String getHouseId() {
        return houseId;
    }

    public void setHouseId(String houseId) {
        this.houseId = houseId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Question toQuestion() {
        return new Question(id, askerId, houseId, text, answer);
    }

    @Override
    public String toString() {
        return "QuestionDAO{" +
                "id='" + id + '\'' +
                ", askerId='" + askerId + '\'' +
                ", houseId='" + houseId + '\'' +
                ", text='" + text + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
