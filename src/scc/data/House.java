package scc.data;

import scc.srv.utils.HasId;

import java.util.Arrays;
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

    private List<String> rentalsID;

    private String ownerId;

    private double[][] priceByPeriod;

    private double discount;


    public House(String id, String name, String location, String description,
                 String photoId, List<String> rentalsID, String ownerId, double[][] priceByPeriod, double discount) {
        super();
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoId = photoId;
        this.rentalsID = rentalsID;
        this.ownerId = ownerId;
        this.priceByPeriod = priceByPeriod;
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

    public List<String> getRentalsID() {
        return rentalsID;
    }

    public void addRental(String rentalID) {
        rentalsID.add(rentalID);
    }

    public void removeRental(String rentalID) {
        rentalsID.remove(rentalID);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public double[][] getPriceByPeriod() {
        return priceByPeriod;
    }

    /**
     * Returns the Price of the given Month for a given period of time.
     *
     * @param month
     * @param period - booking period ( 0 - day; 1 - week; 2 - month )
     * @return the price of the given Month.
     */
    public double getPeriodPrice(int month, int period) {
        return (priceByPeriod[month][period] * (1 - discount));
    }

    /**
     * Change the price of a particular period in a given month.
     *
     * @param month
     * @param period - booking period ( 0 - day; 1 - week; 2 - month )
     * @param newPrice    - the new Price
     */
    public void setPeriodPrice(int month, int period, int newPrice) {
        priceByPeriod[month][period] = newPrice;
    }

    public double getDiscount() { return discount; }

    public void setDiscount(double discount) { this.discount = discount; }

    @Override
    public String toString() {
        return "House{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", photoId='" + photoId + '\'' +
                ", rentalsID=" + rentalsID +
                ", ownerId='" + ownerId + '\'' +
                ", priceByPeriod=" + Arrays.toString(priceByPeriod) +
                '}';
    }
}
