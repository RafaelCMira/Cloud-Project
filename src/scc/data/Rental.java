package scc.data;

import java.util.Date;

/**
 * Represents a House Rental, as returned to the client
 */

public class Rental {
    private String id;
    private String houseId;
    private String userId;
    private double price;
    private Date startDate;
    private Date endDate;
    private double discount;

    public Rental() {
    }

    public Rental(String id, String houseId, String userId, double price, Date startDate, Date endDate, double discount) {
        super();
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
        this.price = price;
        this.startDate = startDate;
        this.endDate = endDate;
        this.discount = discount;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setHouseId(String houseId) {
        this.houseId = houseId;
    }

    public String getHouseId() {
        return houseId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return "Rental{" +
                "id='" + id + '\'' +
                ", houseId='" + houseId + '\'' +
                ", userId='" + userId + '\'' +
                ", price=" + price +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", discount=" + discount +
                '}';
    }
}
