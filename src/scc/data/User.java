package scc.data;

import java.util.ArrayList;
import java.util.Arrays;
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

    public User(String userID, String name, String pwd, String photoId, String[] houseIds) {
        super();
        this.userID = userID;
        this.name = name;
        this.pwd = pwd;
        this.photoId = photoId;
        this.houseIds = Arrays.stream(houseIds).toList();
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

    public String[] getHouseIds() {
        if (houseIds == null)
            houseIds = new ArrayList<>();
        return houseIds.toArray(new String[0]);
    }

    public void addHouse(String houseId) {
        if (! houseIds.contains(houseId))
            houseIds.add(houseId);
    }

    public void setHouseIds(String[] houseIds) {
        this.houseIds = Arrays.stream(houseIds).toList();
    }

    public UserDAO toUserDAO() {
        return new UserDAO(this);
    }

    @Override
    public String toString() {
        return "User [id=" + userID + ", name=" + name + ", pwd=" + pwd + ", photoId=" + photoId + ", houseIds="
                + houseIds.toString() + "]";
    }

}
