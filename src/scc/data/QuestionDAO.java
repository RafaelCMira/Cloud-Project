package scc.data;

public class QuestionDAO {

    private String _rid;
    private String _ts;
    private String id;
    private String userId;
    private String houseId;
    private String text;
    private String answer;

    public QuestionDAO() {
    }

    public QuestionDAO(Question q) {
        this(q.getId(), q.getuserId(), q.getHouseId(), q.getText(), q.getAnswer());
    }

    public QuestionDAO(String id, String userId, String houseId, String text, String answer) {
        super();
        this.id = id;
        this.userId = userId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
        return new Question(id, userId, houseId, text, answer);
    }

    @Override
    public String toString() {
        return "QuestionDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", houseId='" + houseId + '\'' +
                ", text='" + text + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
