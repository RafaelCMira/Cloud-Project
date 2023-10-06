package scc.data;

import java.util.Arrays;

public class HouseDAO {

    private String _rid;

    private String _ts;

    private String id;

    private String name;

    private String location;

    private String description;

    private String photoId;

    private String ownerID;

    public HouseDAO(){}

    public HouseDAO(House h) { this(h.getId(), h.getName(), h.getLocation(), h.getDescription(), h.getPhotoId(), h.getOwnerID()); }

    public HouseDAO(String id, String name, String location, String description , String photoId, String ownerID) {
        super();
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoId = photoId;
        this.ownerID = ownerID;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) { this.description = description;}

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getOwnerID() { return ownerID; }

    public void setOwnerID(String ownerID) { this.ownerID = ownerID;}

    public House toHouse() {
        return new House(id, name, location, description, photoId, ownerID);
    }

    @Override
    public String toString() {
        return "House [ _rid=" + _rid + ", _ts=" + _ts +
                ", id=" + id + ", name=" + name + ", location=" + location + ", description=" + description + ", photoId=" + photoId + ", ownerID="
        + ownerID +"]";
    }

}
