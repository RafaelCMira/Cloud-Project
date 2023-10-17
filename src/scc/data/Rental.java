package scc.data;

import java.time.LocalDate;

/**
 * Represents a House Rental, as returned to the client
 */

public class Rental {
    private String id;
    private String houseID;
    private String userID;
    private double price;
    private LocalDate initialDate;
    private LocalDate endDate;

    public Rental(String id, String houseID, String userID, double price, LocalDate initialDate, LocalDate endDate) {
        super();
        this.id = id;
        this.houseID = houseID;
        this.userID = userID;
        this.price = price;
        this.initialDate = initialDate;
        this.endDate = endDate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setHouseID(String houseID) {
        this.houseID = houseID;
    }

    public String getHouseID() {
        return houseID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserID() {
        return userID;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public void setInitialDate(LocalDate initialDate) {
        this.initialDate = initialDate;
    }

    public LocalDate getInitialDate() {
        return initialDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
