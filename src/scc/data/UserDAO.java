package scc.data;

import java.util.Arrays;

import scc.utils.Hash;

/**
 * Represents a User, as stored in the database
 */
public class UserDAO {
    private String _rid;
    private String _ts;
    private String userID;
    private String name;
    private String pwd;
    private String photoId;
    private String[] houseIds;

    public UserDAO() {
    }

    public UserDAO(User u) {
        this(u.getUserID(), u.getName(), u.getPwd(), u.getPhotoId(), u.getHouseIds());
    }

    public UserDAO(String userID, String name, String pwd, String photoId, String[] houseIds) {
        super();
        this.userID = userID;
        this.name = name;
        this.pwd = Hash.of(pwd);
        this.photoId = photoId;
        this.houseIds = houseIds;
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
        this.pwd = Hash.of(pwd);
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String[] getHouseIds() {
        return houseIds == null ? new String[0] : houseIds;
    }

    public void setHouseIds(String[] houseIds) {
        this.houseIds = houseIds;
    }

    public User toUser() {
        return new User(userID, name, pwd, photoId, houseIds == null ? null : Arrays.copyOf(houseIds, houseIds.length));
    }

    @Override
    public String toString() {
        return "UserDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + userID + ", name=" + name + ", pwd=" + pwd
                + ", photoId=" + photoId + ", houseIds=" + Arrays.toString(houseIds) + "]";
    }

}
