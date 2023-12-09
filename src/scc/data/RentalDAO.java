package scc.data;

import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonId;
import scc.srv.utils.HasId;
import scc.srv.utils.Utility;

import java.time.Instant;
import java.util.Date;

/**
 * Represents a House Rental, as returned to the client
 */

public class RentalDAO implements HasId {

    @BsonId
    private String id;
    private String houseId;
    private String userId;
    private Integer price;
    private Date initialDate;
    private Date endDate;

    public RentalDAO() {
    }

    public RentalDAO(Rental r) {
        this(r.getId(), r.getHouseId(), r.getUserId(), r.getPrice(), Date.from(Instant.parse(r.getInitialDate())),
                Date.from(Instant.parse(r.getEndDate())));
    }

    public RentalDAO(String id, String houseId, String userId, Integer price, Date initialDate, Date endDate) {
        super();
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
        this.price = price;
        this.initialDate = initialDate;
        this.endDate = endDate;
    }

    public static RentalDAO fromDocument(Document document) {
        RentalDAO rentalDAO = new RentalDAO();
        rentalDAO.setId(document.getString("id"));
        rentalDAO.setHouseId(document.getString("houseId"));
        rentalDAO.setUserId(document.getString("userId"));
        rentalDAO.setPrice(document.getInteger("price"));
        rentalDAO.setInitialDate(Utility.formatDate(document.getLong("initialDate")));
        rentalDAO.setEndDate(Utility.formatDate(document.getLong("endDate")));
        return rentalDAO;
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
        return new Rental(id, houseId, userId, price, initialDate, endDate);
    }

    @Override
    public String toString() {
        return "RentalDAO{" +
                "id='" + id + '\'' +
                ", houseId='" + houseId + '\'' +
                ", userId='" + userId + '\'' +
                ", price=" + price +
                ", initialDate=" + initialDate +
                ", endDate=" + endDate +
                '}';
    }
}
