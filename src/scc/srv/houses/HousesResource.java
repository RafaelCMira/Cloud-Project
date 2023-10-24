package scc.srv.houses;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.utils.Checks;
import scc.srv.utils.Cache;
import scc.srv.media.MediaResource;
import scc.srv.users.UsersService;
import scc.utils.Hash;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HousesResource implements HousesService {
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String createHouse(HouseDAO houseDAO) throws Exception {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            var user = checksHouseCreation(houseDAO, jedis);

            user.addHouse(houseDAO.getId());

            var resUpdateUser = db.updateUserById(user.getId(), user);
            int statusCode = resUpdateUser.getStatusCode();

            if (!Checks.isStatusOk(statusCode))
                throw new Exception("Error: " + statusCode);

            var resCreateHouse = db.createHouse(houseDAO);
            statusCode = resCreateHouse.getStatusCode();

            // If all operations succeeded, put in cache the updates
            // TODO: enviar ambos os pedidos para a cache de uma vez, o stor disse que dava para fazer
            if (Checks.isStatusOk(statusCode)) {
                Cache.putInCache(houseDAO, HOUSE_PREFIX, jedis);
                Cache.putInCache(user, UsersService.USER_PREFIX, jedis);
                return houseDAO.toHouse().toString();
            } else {
                throw new Exception("Error: " + statusCode);
            }
        }
    }

    @Override
    public String deleteHouse(String id) throws Exception {
        if (Checks.badParams(id))
            throw new Exception("Error: 400 Bad Request (ID NULL)");

        var item = db.getHouseById(id).stream().findFirst();
        String ownerId;
        if (item.isPresent())
            ownerId = item.get().getOwnerId();
        else
            throw new Exception("Error: 404");

        var res = db.delHouseById(id);
        int statusCode = res.getStatusCode();

        if (!Checks.isStatusOk(statusCode))
            throw new Exception("Error: " + statusCode);

        // TODO: DÁ ERRO PORQUE O DELETE DEVOLVE NULL, PODEMOS TER DE ALTERAR A COSMOS DB e ativar o soft delete
        var user = db.getUserById(ownerId).stream().findFirst();
        if (user.isPresent()) {
            // acho que esta verificaçao do user.isPresent é necessaria para alteraçoes em paralelo
            // (podem remover o user mesmo antes de se começar a fazer o delete da casa)
            var updatedUser = user.get();
            updatedUser.removeHouse(id);
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                //jedis.del(HOUSE_PREFIX + id);
                Cache.deleteFromCache(HOUSE_PREFIX, id, jedis);
                Cache.putInCache(updatedUser, UsersService.USER_PREFIX, jedis);
            }
            return String.format("StatusCode: %d \nHouse %s was delete", statusCode, id);
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }

    @Override
    public House getHouse(String id) throws Exception {
        if (Checks.badParams(id))
            throw new Exception("Error: 400 Bad Request (ID NULL)");

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String house = Cache.getFromCache(HOUSE_PREFIX, id, jedis);
            if (house != null) {
                return mapper.readValue(house, HouseDAO.class).toHouse();
            }

            var res = db.getHouseById(id);
            var result = res.stream().findFirst();

            if (result.isPresent()) {
                var houseToCACHE = result.get();
                Cache.putInCache(houseToCACHE, HOUSE_PREFIX, jedis);
                return houseToCACHE.toHouse();
            } else {
                throw new Exception("Error: 404");
            }
        }
    }

    @Override
    public House updateHouse(String id, House house) throws Exception {
        HouseDAO updatedHouse = refactorHouse(id, house);
        var res = db.updateHouseById(id, updatedHouse);
        int statusCode = res.getStatusCode();
        if (Checks.isStatusOk(statusCode)) {
            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                Cache.putInCache(updatedHouse, HOUSE_PREFIX, jedis);
            }
            return updatedHouse.toHouse();
        } else {
            throw new Exception("Error: " + statusCode);
        }
    }


    @Override
    public List<House> getAvailHouseByLocation(String location) throws Exception {
        //TODO: chache

        List<House> result = new ArrayList<>();

        var res = db.getHousesByLocation(location);
        var houses = res.stream().toList();
        if (houses.isEmpty())
            throw new Exception("There is no available houses in this location: 404");

        // Check if house is available
        // TODO: há uma forma melhor de fazer isto

        for (HouseDAO h : houses) {
            boolean available = true;
            for (String rentalId : h.getRentalsIds()) {
                var rental = db.getRentalById(h.getId(), rentalId).stream().findFirst();
                if (rental.isPresent() && rental.get().getEndDate().isBefore(LocalDate.now()))
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

    private boolean isDateInBetween(LocalDate start, LocalDate end) {
        var currentDate = LocalDate.now();
        return !currentDate.isBefore(start) && !currentDate.isAfter(end);
    }

    private boolean doDatesIntersect(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !end1.isBefore(start2) && !end2.isBefore(start1);
    }


    @Override
    public List<House> getHouseByLocationPeriod(String location, String initialDate, String endDate) throws Exception {
        //TODO: chache
        LocalDate iniDate = LocalDate.parse(initialDate);
        LocalDate eDate = LocalDate.parse(endDate);

        List<House> result = new ArrayList<>();

        CosmosPagedIterable<HouseDAO> res = db.getHousesByLocation(location);
        List<HouseDAO> houses = res.stream().toList();

        // Check if house is available in the given timeframe
        for (HouseDAO h : houses) {
            boolean available = true;
            for (String id : h.getRentalsIds()) {
                Optional<RentalDAO> rental = db.getRentalById(h.getId(), id).stream().findFirst();
                if (rental.isEmpty()) {
                    available = false;
                } else {
                    RentalDAO r = rental.get();
                    available = r.getInitialDate().isAfter(eDate) || r.getEndDate().isBefore(iniDate);
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
     * @param id    of the house being accessed
     * @param house new house attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws Exception If id is null or if the user does not exist
     */
    private HouseDAO refactorHouse(String id, House house) throws Exception {
        if (Checks.badParams(id))
            throw new Exception("Error: 400 Bad Request (ID NULL)");

        var res = db.getHouseById(id);
        var result = res.stream().findFirst();
        if (result.isPresent()) {
            HouseDAO houseDAO = result.get();

            String newName = house.getName();
            if (!newName.isBlank())
                houseDAO.setName(newName);

            String newLocation = Hash.of(house.getLocation());
            if (!newLocation.isBlank())
                houseDAO.setLocation(newLocation);

            String newPhoto = house.getPhotoId();
            MediaResource media = new MediaResource();
            if (!media.hasPhotoById(newPhoto))
                throw new Exception("Error: 404 Image not found");
            else
                houseDAO.setPhotoId(newPhoto);

            String newDescription = house.getDescription();
            if (!newDescription.isBlank())
                houseDAO.setDescription(newDescription);

            return houseDAO;
        } else {
            throw new Exception("Error: 404");
        }
    }

    private UserDAO checksHouseCreation(HouseDAO houseDAO, Jedis jedis) throws Exception {
        if (Checks.badParams(houseDAO.getId(), houseDAO.getName(), houseDAO.getLocation(),
                houseDAO.getPhotoId()) && !hasPricesByPeriod(houseDAO)) {
            throw new Exception("Error: 400 Bad Request");
        }

        // Check if house already exists on Cache
        if (jedis.get(HousesService.HOUSE_PREFIX + houseDAO.getId()) != null)
            throw new Exception("Error: 409 House already exists");

        // Check if house already exists on DB
        Optional<HouseDAO> house = db.getHouseById(houseDAO.getId()).stream().findFirst();
        if (house.isPresent())
            throw new Exception("Error: 409 House already exists");

        MediaResource media = new MediaResource();
        if (!media.hasPhotoById(houseDAO.getPhotoId()))
            throw new Exception("Error: 404 Image not found.");

        // Checks if the user exists in cache
        UserDAO user = mapper.readValue(jedis.get(UsersService.USER_PREFIX + houseDAO.getOwnerId()), UserDAO.class);
        if (user != null) return user;

        // Checks if the user exists in DB
        var dbUser = db.getUserById(houseDAO.getOwnerId()).stream().findFirst();
        if (dbUser.isEmpty())
            throw new Exception("Error: 404 User not found.");

        return dbUser.get();
    }


}
