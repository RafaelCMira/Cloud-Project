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

    private List<String> rentalsID;

    private String ownerID;

    private int[][] priceByPeriod;


    public House(String id, String name, String location, String description,
                 String photoId, List<String> rentalsID, String ownerID, int[][] priceByPeriod) {
        super();
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoId = photoId;
        this.rentalsID = rentalsID;
        this.ownerID = ownerID;
        this.priceByPeriod = priceByPeriod;
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

    public String getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    public int[][] getPriceByPeriod() {
        return priceByPeriod;
    }

    /**
     * Returns the Price of the given Month, it can be the discount or the normal price.
     *
     * @param typeOfPrice - type of the Price ( 0 = Normal Price; 1 = Discount Price)
     * @param month
     * @return the price of the given Month.
     */
    public int getPeriodPrice(int typeOfPrice, int month) {
        return priceByPeriod[typeOfPrice][month];
    }

    /**
     * Change the price of a particular type in a given month.
     *
     * @param typeOfPrice - type of the Price ( 0 = Normal Price; 1 = Discount Price)
     * @param month
     * @param newPrice    - the new Price
     */
    public void setPeriodPrice(int typeOfPrice, int month, int newPrice) {
        priceByPeriod[typeOfPrice][month] = newPrice;
    }

    @Override
    public String toString() {
        return "House [id=" + id + ", name=" + name + ", location=" + location + ", description=" + description + ", photoId=" + photoId + ", ownerID="
                + ownerID + "]";
    }


}
