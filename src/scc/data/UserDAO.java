package scc.data;

import java.util.List;

import scc.srv.utils.HasId;
import scc.utils.Hash;

/**
 * Represents a User, as stored in the database
 */
public class UserDAO implements HasId {
    private String _rid;
    private String _ts;
    private String id;
    private String name;
    private String pwd;
    private String photoId;
    private List<String> houseIds;

    public UserDAO() {
    }

    public UserDAO(User u) {
        this(u.getId(), u.getName(), u.getPwd(), u.getPhotoId(), u.getHouseIds());
    }

    public UserDAO(String userID, String name, String pwd, String photoId, List<String> houseIds) {
        super();
        this.id = userID;
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

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return houseIds;
    }

    public void setHouseIds(List<String> houseIds) {
        this.houseIds = houseIds;
    }

    public void addHouse(String houseId) {
        if (!houseIds.contains(houseId))
            houseIds.add(houseId);
    }

    public void removeHouse(String houseId) {
        houseIds.remove(houseId);
    }

    public User toUser() {
        return new User(id, name, pwd, photoId, houseIds);
    }

    @Override
    public String toString() {
        return "UserDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                ", photoId='" + photoId + '\'' +
                ", houseIds=" + houseIds +
                '}';
    }
}
