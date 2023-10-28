package scc.data;

/**
 * Represents a discussion (one question with only one answer) between a customer and the owner,
 * as returned to the client
 */

public class Question {

    private String id;
    private String askerId;
    private String houseId;
    private String text;
    private String answer;

    public Question() {
    }

    public Question(String id, String askerId, String houseId, String text, String answer) {
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


    @Override
    public String toString() {
        return "Question{" +
                "id='" + id + '\'' +
                ", askerId='" + askerId + '\'' +
                ", houseId='" + houseId + '\'' +
                ", text='" + text + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
