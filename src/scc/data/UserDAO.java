package scc.data;

import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonId;
import scc.srv.utils.HasId;
import scc.utils.Hash;

/**
 * Represents a User, as stored in the database
 */
public class UserDAO implements HasId {

    @BsonId
    private String id;
    private String name;
    private String pwd;
    private String photoId;

    public UserDAO() {
    }

    public UserDAO(User u) {
        this(u.getId(), u.getName(), u.getPwd(), u.getPhotoId());
    }

    public UserDAO(String userID, String name, String pwd, String photoId) {
        super();
        this.id = userID;
        this.name = name;
        this.pwd = Hash.of(pwd);
        this.photoId = photoId;
    }

    public static UserDAO fromDocument(Document document) {
        UserDAO userDAO = new UserDAO();
        userDAO.setId(document.getString("id"));
        userDAO.setName(document.getString("name"));
        userDAO.setPwd(document.getString("pwd"));
        userDAO.setPhotoId(document.getString("photoId"));
        return userDAO;
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


    public User toUser() {
        return new User(id, name, pwd, photoId);
    }

    @Override
    public String toString() {
        return "UserDAO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                ", photoId='" + photoId + '\'' +
                '}';
    }
}
