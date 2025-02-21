package scc.data;

import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonId;
import scc.srv.utils.HasId;

import java.util.ArrayList;
import java.util.List;

public class HouseDAO implements HasId {

    @BsonId
    private String id;

    private String name;

    private String location;

    private String description;

    private List<String> photosIds;

    private String ownerId;

    private Integer price;

    private Integer discount;

    private Integer rentalsCounter;

    public HouseDAO() {
    }

    public HouseDAO(House h) {
        this(h.getId(), h.getName(), h.getLocation(), h.getDescription(),
                h.getPhotosIds(), h.getOwnerId(), h.getPrice(), h.getDiscount(), h.getRentalsCounter());
    }

    public HouseDAO(String id, String name, String location, String description, List<String> photosIds,
                    String ownerId, Integer price, Integer discount, Integer rentalsCounter) {
        super();
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photosIds = photosIds;
        this.ownerId = ownerId;
        this.price = price;
        this.discount = discount;
        this.rentalsCounter = rentalsCounter;
    }

    public static HouseDAO fromDocument(Document document) {
        HouseDAO houseDAO = new HouseDAO();
        houseDAO.setId(document.getString("id"));
        houseDAO.setName(document.getString("name"));
        houseDAO.setLocation(document.getString("location"));
        houseDAO.setDescription(document.getString("description"));
        houseDAO.setPhotosIds(document.getList("photosIds", String.class, new ArrayList<>()));
        houseDAO.setOwnerId(document.getString("ownerId"));
        houseDAO.setPrice(document.getInteger("price"));
        houseDAO.setDiscount(document.getInteger("discount"));
        houseDAO.setRentalsCounter(document.getInteger("rentalsCounter"));
        return houseDAO;
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

    public List<String> getPhotosIds() {
        return photosIds;
    }

    public void setPhotosIds(List<String> photosIds) {
        this.photosIds = photosIds;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getDiscount() {
        return discount;
    }

    public void setDiscount(Integer discount) {
        this.discount = discount;
    }

    public House toHouse() {
        return new House(id, name, location, description, photosIds, ownerId, price, discount, rentalsCounter);
    }

    public Integer getRentalsCounter() {
        return rentalsCounter;
    }

    public void setRentalsCounter(Integer rentalsCounter) {
        this.rentalsCounter = rentalsCounter;
    }

    @Override
    public String toString() {
        return "HouseDAO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", photosIds=" + photosIds +
                ", ownerId='" + ownerId + '\'' +
                ", price=" + price +
                ", discount=" + discount +
                ", rentalsCounter=" + rentalsCounter +
                '}';
    }
}
