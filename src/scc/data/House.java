package scc.data;

import scc.srv.utils.HasId;

import java.util.List;

/**
 * Represents a House, as returned to the clients
 */

public class House implements HasId {

    private String id;

    private String name;

    private String location;

    private String description;

    private List<String> photosIds;

    private String ownerId;

    private Integer price;

    private Integer discount;

    private Integer rentalsCounter;

    public House() {
    }

    public House(String id, String name, String location, String description,
                 List<String> photosIds, String ownerId, Integer price, Integer discount, Integer rentalsCounter) {
        super();
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photosIds = photosIds;
        this.ownerId = ownerId;
        this.price = price;
        this.discount = discount;
        this.rentalsCounter = rentalsCounter;
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

    public Integer getRentalsCounter() {
        return rentalsCounter;
    }

    public void setRentalsCounter(Integer rentalsCounter) {
        this.rentalsCounter = rentalsCounter;
    }

    @Override
    public String toString() {
        return "House{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", photosIds=" + photosIds +
                ", ownerId='" + ownerId + '\'' +
                ", price=" + price +
                ", discount=" + discount +
                ", rentalsCounter=" + rentalsCounter +
                '}';
    }
}
