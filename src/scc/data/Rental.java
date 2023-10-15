package scc.data;

import java.time.LocalDate;

/**
 * Represents a House Rental, as returned to the client
 */

public class Rental {
    private int rentalID;
    private House rental;
    private User renter;
    private double price;
    private LocalDate initialDate;
    private LocalDate endDate;

    public Rental(int rentalID, House rental, User renter, double price, LocalDate initialDate, LocalDate endDate) {
        super();
        this.rentalID = rentalID;
        this.rental = rental;
        this.renter = renter;
        this.price = price;
        this.initialDate = initialDate;
        this.endDate = endDate;
    }

    public void setRentalID(int rentalID) {
        this.rentalID = rentalID;
    }

    public int getRentalID() {
        return rentalID;
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
}
