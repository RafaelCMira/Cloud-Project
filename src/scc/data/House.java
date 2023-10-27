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

    private String photoId;

    private List<String> rentalsIds;

    private String ownerId;

    private int price;

    private double discount;


    public House(String id, String name, String location, String description,
                 String photoId, List<String> rentalsIds, String ownerId, int price, double discount) {
        super();
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoId = photoId;
        this.rentalsIds = rentalsIds;
        this.ownerId = ownerId;
        this.price = price;
        this.discount = discount;
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

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public List<String> getRentalsIds() {
        return rentalsIds;
    }

    public void addRental(String rentalID) {
        rentalsIds.add(rentalID);
    }

    public void removeRental(String rentalID) {
        rentalsIds.remove(rentalID);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return "House{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", photoId='" + photoId + '\'' +
                ", rentalsID=" + rentalsIds +
                ", ownerId='" + ownerId + '\'' +
                ", price=" + price +
                ", discount=" + discount +
                '}';
    }
}
