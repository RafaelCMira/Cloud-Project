package scc.data;

import java.time.LocalDate;

/**
 * Represents a House Rental, as returned to the client
 */

public class Rental {
    private String id;
    private String houseId;
    private String askerId;
    private double price;
    private LocalDate startDate;
    private LocalDate endDate;
    private double discount;

    public Rental() {
    }

    public Rental(String id, String houseId, String askerId, double price, LocalDate startDate, LocalDate endDate, double discount) {
        super();
        this.id = id;
        this.houseId = houseId;
        this.askerId = askerId;
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

    public void setAskerId(String askerId) {
        this.askerId = askerId;
    }

    public String getAskerId() {
        return askerId;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getEndDate() {
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
                ", askerId='" + askerId + '\'' +
                ", price=" + price +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", discount=" + discount +
                '}';
    }
}
