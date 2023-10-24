package scc.data;

/**
 * Represents a discussion (one question with only one answer) between a customer and the owner,
 * as returned to the client
 */

public class Question {

    private String questionId;
    private String userId;
    private String houseId;
    private String text;
    private String answer;

    public Question(String questionId, String userId, String houseId, String text, String answer) {
        super();
        this.questionId = questionId;
        this.userId = userId;
        this.houseId = houseId;
        this.text = text;
        this.answer = answer;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getuserId() {
        return userId;
    }

    public void setuserId(String userId) {
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

    @Override
    public String toString() {
        return "Question{" +
                "questionId='" + questionId + '\'' +
                ", userId='" + userId + '\'' +
                ", houseId='" + houseId + '\'' +
                ", text='" + text + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
