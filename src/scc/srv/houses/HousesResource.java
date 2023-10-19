package scc.srv.houses;

import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.House;
import scc.data.HouseDAO;
import scc.data.User;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.Checks;
import scc.srv.media.MediaResource;
import scc.srv.users.UsersResource;
import scc.utils.Hash;

import java.util.Optional;

public class HousesResource implements HousesService {
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String createHouse(HouseDAO houseDAO) throws Exception {
        if (Checks.badParams(houseDAO.getId(), houseDAO.getName(), houseDAO.getLocation(),
                houseDAO.getPhotoId()) && !hasPricesByPeriod(houseDAO)) {
            throw new Exception("Error: 400 Bad Request");
        }

        MediaResource media = new MediaResource();
        if (!media.hasPhotoById(houseDAO.getPhotoId()))
            throw new Exception("Error: 404 Image not found.");

        Optional<UserDAO> user = db.getUserById(houseDAO.getOwnerID()).stream().findFirst();
        if(user.isEmpty())
            throw new Exception("Error: 404 User not found.");

        var res = db.createHouse(houseDAO);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(statusCode)) {
            updateOwner(houseDAO.getId(),houseDAO.getOwnerID());
            return houseDAO.toHouse().toString();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public String deleteHouse(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");

        CosmosItemResponse<Object> res = db.delHouseById(id);
        int statusCode = res.getStatusCode();

        if (Checks.isStatusOk(statusCode)) {
            return String.format("StatusCode: %d \nHouse %s was delete", statusCode, id);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public House getHouse(String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");

        CosmosPagedIterable<HouseDAO> res = db.getHouseById(id);
        Optional<HouseDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            return result.get().toHouse();
        } else {
            throw new Exception("Error: 404");
        }
    }

    @Override
    public House updateHouse(String id, HouseDAO houseDAO) throws Exception {
        HouseDAO updatedHouse = refactorHouse(id, houseDAO);
        var res = db.updateHouseById(id, updatedHouse);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(res.getStatusCode())) {
            return updatedHouse.toHouse();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

/*
    @Override
    public List<House> getAvailHouseByLocation(String location) throws Exception {
        var cDate = java.time.LocalDate.now(); // Get current date
        List<House> result = new ArrayList<>();

        CosmosPagedIterable<HouseDAO> res = db.getHousesLocation(location);
        List<HouseDAO> houses = res.stream().toList();

        // Check if house is available
         for ( HouseDAO h: houses) {
             boolean available = true;
            for (String id: h.getRentalsID()) {
                Optional<RentalDAO>  rental = db.getRentalById(id).stream().findFirst();
                if (rental.isEmpty() || rental.get().getInitialDate() != cDate)
                    available = false;
            }
            if (available)
                result.add(h.toHouse());
        }

        if (!result.isEmpty()) {
            return result;
        } else {
            throw new Exception("Error: 404");
        }
    }

    @Override
    public List<House> getHouseByLocationPeriod(String location, LocalDate initialDate, LocalDate endDate) throws Exception {
        List<House> result = new ArrayList<>();

        CosmosPagedIterable<HouseDAO> res = db.getHousesLocation(location);
        List<HouseDAO> houses = res.stream().toList();

        // Check if house is available in the given timeframe
        for ( HouseDAO h: houses) {
            boolean available = true;
            for (String id: h.getRentalsID()) {
                Optional<RentalDAO>  rental = db.getRentalById(id).stream().findFirst();
                if (rental.isEmpty()) {
                    available = false;
                } else {
                    RentalDAO r = rental.get();
                    available = r.getInitialDate().isAfter(endDate) || r.getEndDate().isBefore(initialDate);
                }

            }
            if (available)
                result.add(h.toHouse());
        }

        if (!result.isEmpty()) {
            return result;
        } else {
            throw new Exception("Error: 404");
        }
    }
*/


    /**
     * Checks if the given House has prices valid for all months.
     *
     * @param house - house to be checked
     * @return true if all month has a valid price, false otherwise.
     */
    private boolean hasPricesByPeriod(HouseDAO house) {
        int[][] p = house.getPriceByPeriod();
        for (int i = 0; i < 12; i++) {
            if (p[0][i] <= 0) return false;
        }
        return true;
    }

    /**
     * Returns updated houseDAO to the method who's making the request to the database
     *
     * @param id       of the house being accessed
     * @param houseDAO new house attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws Exception If id is null or if the user does not exist
     */
    private HouseDAO refactorHouse(String id, HouseDAO houseDAO) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (ID NULL)");
        CosmosPagedIterable<HouseDAO> res = db.getHouseById(id);
        Optional<HouseDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            HouseDAO h = result.get();

            String houseDAOName = houseDAO.getName();
            if (!h.getName().equals(houseDAOName))
                h.setName(houseDAOName);

            String houseDAOloc = Hash.of(houseDAO.getLocation());
            if (!h.getLocation().equals(houseDAOloc))
                h.setLocation(houseDAOloc);

            String houseDAOPhotoId = houseDAO.getPhotoId();
            if (!h.getPhotoId().equals(houseDAOPhotoId))
                h.setPhotoId(houseDAOPhotoId);

            // TODO - finish refactor

            return h;

        } else {
            throw new Exception("Error: 404");
        }
    }

    /**
     * Adds the house Id to the uses houses list and updates the DB
     * @param houseId - the house id
     * @param ownerId - the owner id
     */
    private void updateOwner(String houseId, String ownerId) throws Exception{
        UsersResource uResource = new UsersResource();

        User user = uResource.getUser(ownerId);
        user.addHouse(houseId);
        uResource.updateUser(ownerId,user);
    }



}
