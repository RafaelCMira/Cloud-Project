package scc.data;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents a House Rental, as returned to the client
 */

public class Rental {
    private String id;
    private String houseId;
    private String userId;
    private double price;
    private double discount;
    private String initialDate;
    private String endDate;

    public Rental() {
    }

    public Rental(String id, String houseId, String userId, double price, Date initialDate, Date endDate, double discount) {
        super();
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
        this.price = price;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.initialDate = dateFormat.format(initialDate);
        this.endDate = dateFormat.format(endDate);

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

    public String getInitialDate() {
        return initialDate;
    }

    public void setInitialDate(String initialDate) {
        this.initialDate = initialDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
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
                ", startDate=" + initialDate +
                ", endDate=" + endDate +
                ", discount=" + discount +
                '}';
    }
}
