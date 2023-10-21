package scc.data;

import java.time.LocalDate;

/**
 * Represents a House Rental, as returned to the client
 */

public class RentalDAO {
    private String _rid;
    private String _ts;
    private String id;
    private String houseID;
    private String userID;
    private double price;
    private double discount;
    private LocalDate initialDate;
    private LocalDate endDate;

    public RentalDAO(Rental r) {
        this(r.getId(), r.getHouseID(), r.getUserID(), r.getPrice(), r.getInitialDate(),
                r.getEndDate(), r.getDiscount());
    }

    public RentalDAO(String id, String houseID, String userID, double price, LocalDate initialDate, LocalDate endDate, double discount) {
        super();
        this.id = id;
        this.houseID = houseID;
        this.userID = userID;
        this.price = price;
        this.initialDate = initialDate;
        this.endDate = endDate;
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

    public Rental toRental() { return new Rental(id, houseID, userID, price, initialDate, endDate, discount);}

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    @Override
    public String toString() {
        return "Rental [ _rid=" + _rid + ", _ts=" + _ts +
                ", id=" + id + ", houseId=" + houseID + ", userID=" + userID + ", price=" + price + ", initialDate=" + initialDate
                + ", endDate=" + endDate + ", discount=" + discount + " ]";
    }
}
