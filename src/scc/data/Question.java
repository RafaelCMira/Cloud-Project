package scc.data;

/**
 * Represents a discussion (one question with only one answer) between a customer and the owner,
 * as returned to the client
 */

public class Question {

    private String userID;
    private String houseID;
    private String text;
    private String answer;

    public Question(String userID, String houseID, String text, String answer) {
        super();
        this.userID = userID;
        this.houseID = houseID;
        this.text = text;
        this.answer = answer;
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

    @Override
    public String toString() {
        return "Question{" +
                "userID='" + userID + '\'' +
                ", houseID='" + houseID + '\'' +
                ", text='" + text + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
