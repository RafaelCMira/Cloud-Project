package scc.data;

import scc.srv.utils.HasId;

import java.util.Arrays;
import java.util.List;

public class HouseDAO implements HasId {

    private String _rid;

    private String _ts;

    private String id;

    private String name;

    private String location;

    private String description;

    private String photoId;

    private List<String> rentalsIds;

    private String ownerId;

    private double[][] priceByPeriod;

    private double discount;

    public HouseDAO() {
    }

    public HouseDAO(House h) {
        this(h.getId(), h.getName(), h.getLocation(), h.getDescription(),
                h.getPhotoId(), h.getRentalsID(), h.getOwnerId(), h.getPriceByPeriod(), h.getDiscount());
    }

    public HouseDAO(String id, String name, String location, String description, String photoId,
                    List<String> rentalsIds, String ownerId, double[][] priceByPeriod, double discount) {
        super();
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoId = photoId;
        this.rentalsIds = rentalsIds;
        this.ownerId = ownerId;
        this.priceByPeriod = priceByPeriod;
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

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public List<String> getRentalsIds() {
        return rentalsIds;
    }

    public void addRental(String rentalID) {
        rentalsIds.add(rentalID);
    }

    public void removeRental(String rentalID) {
        rentalsIds.remove(rentalID);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public double[][] getPriceByPeriod() {
        return priceByPeriod;
    }

    /**
     * Returns the Price of the given Month, it can be the discount or the normal price.
     *
     * @param month ( 0 - January ... 11 - December )
     * @param period - booking period ( 0 - day; 1 - week; 2 - month )
     * @return the price of the given Month.
     */
    public double getPeriodPrice(int month, int period) {
        return (priceByPeriod[month][period] * (1 - discount));
    }

    /**
     * Change the price of a particular type in a given month.
     *
     * @param month ( 0 - January ... 11 - December )
     * @param period - booking period ( 0 - day; 1 - week; 2 - month )
     * @param newPrice - the new Price
     */
    public void setPeriodPrice(int month, int period, int newPrice) {
        priceByPeriod[month][period] = newPrice;
    }

    public House toHouse() {
        return new House(id, name, location, description, photoId, rentalsIds, ownerId, priceByPeriod, discount);
    }

    @Override
    public String toString() {
        return "HouseDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", photoId='" + photoId + '\'' +
                ", rentalsIds=" + rentalsIds +
                ", ownerId='" + ownerId + '\'' +
                ", priceByPeriod=" + Arrays.toString(priceByPeriod) +
                ", discount='" + discount + '\'' +
                '}';
    }
}
