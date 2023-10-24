package scc.srv.rentals;

import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.users.UsersResource;
import scc.srv.utils.Checks;
import scc.srv.houses.HousesService;
import scc.srv.users.UsersService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RentalResource implements RentalService {
    public static final String CONTAINER = "rentals";
    public static final String PARTITION_KEY = "/id";

    private final CosmosDBLayer db = CosmosDBLayer.getInstance();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String createRental(String houseId, RentalDAO rentalDAO) throws Exception {
        if (Checks.badParams(rentalDAO.getId(), rentalDAO.getHouseId(), rentalDAO.getUserId()))
            throw new Exception("Error: 400 Bad Request");

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            //TODO: adicionar o rental à casa

            // Verify if house exists
            String houseJson = jedis.get(HousesService.HOUSE_PREFIX + houseId);
            if (houseJson == null) {
                var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class);

                var hResult = houseRes.stream().findFirst();
                if (hResult.isEmpty())
                    throw new Exception("Error: 404 House Not Found ");
            }

            // Verify if user exists
            if (jedis.get(UsersService.USER_PREFIX + rentalDAO.getUserId()) == null) {
                var userRes = db.getById(rentalDAO.getUserId(), UsersResource.CONTAINER, UserDAO.class).stream().findFirst();
                if (userRes.isEmpty())
                    throw new Exception("Error: 404 User Not Found");
            }

            // TODO - check if the house is available
            if (houseJson != null) {

            }

            var createRental = db.createItem(rentalDAO, CONTAINER);
            int statusCode = createRental.getStatusCode();

            if (Checks.isStatusOk(statusCode)) {
                jedis.set(CACHE_PREFIX + rentalDAO.getId(), mapper.writeValueAsString(rentalDAO));
                return rentalDAO.toRental().toString();
            } else
                throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public Rental getRental(String houseID, String id) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (Null ID)");

        // Check if it is in Cache
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            String rCache = jedis.get(CACHE_PREFIX + id);
            if (rCache != null)
                return mapper.readValue(rCache, RentalDAO.class).toRental();
        }

        //TODO: se nao estiver na chache por lá

        CosmosPagedIterable<RentalDAO> res = db.getRentalById(houseID, id);
        Optional<RentalDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            return result.get().toRental();
        } else {
            throw new Exception("Error: 404 Rental Not Found");
        }
    }

    @Override
    public Rental updateRental(String houseID, String id, RentalDAO rentalDAO) throws Exception {
        RentalDAO updatedRental = refactorRental(houseID, id, rentalDAO);
        var res = db.updateRentalById(id, updatedRental);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(res.getStatusCode())) {
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                jedis.set(CACHE_PREFIX + id, mapper.writeValueAsString(updatedRental));
            }
            return updatedRental.toRental();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public List<Rental> listRentals(String houseID) {
        //TODO: chache ?
        var res = db.getItems(CONTAINER, RentalDAO.class).stream().map(RentalDAO::toRental).toList();
        if (!res.isEmpty())
            return res;
        else
            return new ArrayList<>();
    }

    @Override
    public String deleteRental(String houseId, String id) throws Exception {
        if (id == null)
            throw new Exception("Error: 400 Bad Request (Null ID");

        var res = db.deleteById(id, CONTAINER, houseId);
        int statusCode = res.getStatusCode();

        if (Checks.isStatusOk(statusCode)) {
            HouseDAO houseDAO = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst().get();
            houseDAO.removeRental(id);
            //TODO: delete do rental na casa chache
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                jedis.del(CACHE_PREFIX + id);
            }
            return String.format("StatusCode: %d \nRental %s was delete", statusCode, id);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }


    private RentalDAO refactorRental(String houseID, String id, RentalDAO rentalDAO) throws Exception {
        if (id == null) throw new Exception("Error: 400 Bad Request (Null ID)");

        CosmosPagedIterable<RentalDAO> res = db.getRentalById(houseID, id);
        Optional<RentalDAO> result = res.stream().findFirst();
        if (result.isPresent()) {
            RentalDAO r = result.get();

            String rentalDAOId = rentalDAO.getId();
            if (!r.getId().equals(rentalDAOId))
                r.setId(rentalDAOId);

            String rentalDAOHouseId = rentalDAO.getHouseId();
            if (!r.getHouseId().equals(rentalDAOHouseId))
                r.setHouseId(rentalDAOHouseId);

            String rentalDAOUserId = rentalDAO.getUserId();
            if (!r.getUserId().equals(rentalDAOUserId))
                r.setUserId(rentalDAOUserId);

            double rentalDAOPrice = rentalDAO.getPrice();
            if (r.getPrice() != (rentalDAOPrice))
                r.setPrice(rentalDAOPrice);

            LocalDate rentalDAOInitialDate = rentalDAO.getInitialDate();
            if (!r.getInitialDate().equals(rentalDAOInitialDate))
                r.setInitialDate(rentalDAOInitialDate);

            LocalDate rentalDAOEndDate = rentalDAO.getEndDate();
            if (!r.getEndDate().equals(rentalDAOEndDate))
                r.setEndDate(rentalDAOEndDate);

            return r;

        } else {
            throw new Exception("Error: 404 Rental Not Found");
        }
    }

    @Override
    public List<Rental> getDiscountedRentals(String houseID) throws Exception {
        //TODO: chache & tem ser updated de x em x tempo
        var rentalsDAO = db.getRentals(houseID);
        List<Rental> res = new ArrayList<>();
        for (RentalDAO r : rentalsDAO) {
            if (r.getDiscount() > 0 && !r.getInitialDate().isBefore(LocalDate.now()))
                res.add(r.toRental());
        }
        return res;
    }
}
