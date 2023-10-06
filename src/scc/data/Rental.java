package scc.data;

import java.time.LocalDate;

/**
 * Represents a House Rental, as returned to the client
 */

public class Rental {
    private int id;
    private House rental;
    private User renter;
    private double price;
    private LocalDate initialDate;
    private LocalDate endDate;

    public Rental() {

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

    public House rental() {
        return rental;
    }

    public void setRenter(User renter) {
        this.renter = renter;
    }

    public User renter() {
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
}
