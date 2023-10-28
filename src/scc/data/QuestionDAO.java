package scc.data;

public class QuestionDAO {

    private String _rid;
    private String _ts;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String get_rid() {
        return _rid;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
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
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", askerId='" + askerId + '\'' +
                ", houseId='" + houseId + '\'' +
                ", text='" + text + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
