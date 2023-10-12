package scc.data;

import java.time.LocalDate;

/**
 * Represents a House Rental, as returned to the client
 */

public class RentalDAO {
    private String _rid;
    private String _ts;
    private int id;
    private House rental;
    private User renter;
    private double price;
    private LocalDate initialDate;
    private LocalDate endDate;

    public RentalDAO(Rental r) {this(r.getId(), r.getRental(), r.getRenter(), r.getPrice(), r.getInitialDate(),
            r.getEndDate());}

    public RentalDAO(int id, House rental, User renter, double price, LocalDate initialDate, LocalDate endDate) {
        super();
        this.id = id;
        this.rental = rental;
        this.renter = renter;
        this.price = price;
        this.initialDate = initialDate;
        this.endDate = endDate;
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

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setRental(House rental) {
        this.rental = rental;
    }

    public House getRental() {
        return rental;
    }

    public void setRenter(User renter) {
        this.renter = renter;
    }

    public User getRenter() {
        return renter;
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

    @Override
    public String toString() {
        return "Rental [ _rid=" + _rid + ", _ts=" + _ts +
                ", id=" + id + ", rental=" + rental + ", renter=" + renter + ", price=" + price + ", initialDate=" + initialDate
                + ", endDate=" + endDate +"]";
    }
}
