package scc.data;

import java.util.List;

/**
 * Represents a User, as returned to the clients
 * <p>
 * NOTE: array of house ids is shown as an example of how to store a list of elements and
 * handle the empty list.
 */
public class User {
    private String userID;
    private String name;
    private String pwd;
    private String photoId;
    private List<String> houseIds;

    public User() {
        // Construtor padrão sem argumentos
    }

    public User(String userID, String name, String pwd, String photoId, List<String> houseIds) {
        super();
        this.userID = userID;
        this.name = name;
        this.pwd = pwd;
        this.photoId = photoId;
        this.houseIds = houseIds;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public List<String> getHouseIds() {
        return houseIds == null ? List.of(new String[0]) : houseIds;
    }

    public void setHouseIds(List<String> houseIds) {
        this.houseIds = houseIds;
    }

    @Override
    public String toString() {
        return "User{" +
                "userID='" + userID + '\'' +
                ", name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                ", photoId='" + photoId + '\'' +
                ", houseIds=" + houseIds +
                '}';
    }
}
