package scc.data;

import scc.srv.utils.HasId;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents a House Rental, as returned to the client
 */

public class Rental implements HasId {
    private String id;
    private String houseId;
    private String userId;
    private Integer price;
    private String initialDate;
    private String endDate;

    public Rental() {
    }

    public Rental(String id, String houseId, String userId, Integer price, Date initialDate, Date endDate) {
        super();
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
        this.price = price;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.initialDate = dateFormat.format(initialDate);
        this.endDate = dateFormat.format(endDate);
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

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getPrice() {
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

    @Override
    public String toString() {
        return "Rental{" +
                "id='" + id + '\'' +
                ", houseId='" + houseId + '\'' +
                ", userId='" + userId + '\'' +
                ", price=" + price +
                ", initialDate='" + initialDate + '\'' +
                ", endDate='" + endDate + '\'' +
                '}';
    }
}
