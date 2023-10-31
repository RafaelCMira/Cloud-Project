package scc.data;

import scc.srv.utils.HasId;

import java.util.List;

public class HouseDAO implements HasId {

    private String _rid;

    private String _ts;

    private String id;

    private String name;

    private String location;

    private String description;

    private List<String> photosIds;

    private List<String> rentalsIds;

    private String ownerId;

    private Integer price;

    private Integer discount;

    public HouseDAO() {
    }

    public HouseDAO(House h) {
        this(h.getId(), h.getName(), h.getLocation(), h.getDescription(),
                h.getPhotosIds(), h.getRentalsIds(), h.getOwnerId(), h.getPrice(), h.getDiscount());
    }

    public HouseDAO(String id, String name, String location, String description, List<String> photosIds,
                    List<String> rentalsIds, String ownerId, Integer price, Integer discount) {
        super();
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photosIds = photosIds;
        this.rentalsIds = rentalsIds;
        this.ownerId = ownerId;
        this.price = price;
        this.discount = discount;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPhotosIds() {
        return photosIds;
    }

    public void setPhotosIds(List<String> photosIds) {
        this.photosIds = photosIds;
    }

    public List<String> getRentalsIds() {
        return rentalsIds;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getDiscount() {
        return discount;
    }

    public void setDiscount(Integer discount) {
        this.discount = discount;
    }

    public House toHouse() {
        return new House(id, name, location, description, photosIds, rentalsIds, ownerId, price, discount);
    }

    @Override
    public String toString() {
        return "HouseDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", photosIds=" + photosIds +
                ", rentalsIds=" + rentalsIds +
                ", ownerId='" + ownerId + '\'' +
                ", price=" + price +
                ", discount=" + discount +
                '}';
    }

    public void addRental(String rentalID) {
        rentalsIds.add(rentalID);
    }

    public void removeRental(String rentalID) {
        rentalsIds.remove(rentalID);
    }


}
