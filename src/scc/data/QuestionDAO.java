package scc.data;

public class QuestionDAO {

    private String _rid;
    private String _ts;
    private String questionID;
    private String userID;
    private String houseID;
    private String text;
    private String answer;

    public QuestionDAO() {
    }

    public QuestionDAO(Question q) {
        this(q.getQuestionID(), q.getUserID(), q.getHouseID(), q.getText(), q.getAnswer());
    }

    public QuestionDAO(String questionID, String userID, String houseID, String text, String answer) {
        super();
        this.questionID = questionID;
        this.userID = userID;
        this.houseID = houseID;
        this.text = text;
        this.answer = answer;
    }

    public String getQuestionID() {
        return questionID;
    }

    public void setQuestionID(String questionID) {
        this.questionID = questionID;
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

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getHouseID() {
        return houseID;
    }

    public void setHouseID(String houseID) {
        this.houseID = houseID;
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
        return new Question(questionID, userID, houseID, text, answer);
    }

    @Override
    public String toString() {
        return "QuestionDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", questionID='" + questionID + '\'' +
                ", userID='" + userID + '\'' +
                ", houseID='" + houseID + '\'' +
                ", text='" + text + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
