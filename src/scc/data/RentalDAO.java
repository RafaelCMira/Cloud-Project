package scc.data;

import scc.srv.utils.HasId;

import java.util.Date;

/**
 * Represents a House Rental, as returned to the client
 */

public class RentalDAO implements HasId {
    private String _rid;
    private String _ts;
    private String id;
    private String houseId;
    private String userId;
    private double price;
    private double discount;
    private Date initialDate;
    private Date endDate;

    public RentalDAO() {
    }

    public RentalDAO(Rental r) {
        this(r.getId(), r.getHouseId(), r.getUserId(), r.getPrice(), r.getInitialDate(),
                r.getEndDate(), r.getDiscount());
    }

    public RentalDAO(String id, String houseId, String userId, double price, Date initialDate, Date endDate, double discount) {
        super();
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
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

    public void setInitialDate(Date initialDate) {
        this.initialDate = initialDate;
    }

    public Date getInitialDate() {
        return initialDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Rental toRental() {
        return new Rental(id, houseId, userId, price, initialDate, endDate, discount);
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return "RentalDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", houseId='" + houseId + '\'' +
                ", userId='" + userId + '\'' +
                ", price=" + price +
                ", discount=" + discount +
                ", initialDate=" + initialDate +
                ", endDate=" + endDate +
                '}';
    }
}
