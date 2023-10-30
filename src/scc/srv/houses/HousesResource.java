package scc.srv.houses;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.data.*;
import scc.db.CosmosDBLayer;
import scc.srv.users.UsersResource;
import scc.srv.utils.Cache;
import scc.srv.media.MediaResource;
import scc.srv.users.UsersService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static scc.srv.utils.Utility.*;

public class HousesResource implements HousesService {

    public static final String CONTAINER = "houses";
    public static final String PARTITION_KEY = "/id";
    private final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Response createHouse(HouseDAO houseDAO) throws JsonProcessingException, WebApplicationException {
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            var user = checksHouseCreation(houseDAO, jedis);
            user.addHouse(houseDAO.getId());

            db.updateUser(user);

            db.createItem(houseDAO, CONTAINER);

            // TODO: enviar ambos os pedidos para a cache de uma vez, o stor disse que dava para fazer
            Cache.putInCache(houseDAO, HOUSE_PREFIX, jedis);
            Cache.putInCache(user, UsersService.USER_PREFIX, jedis);

            return sendResponse(OK, houseDAO.toHouse().toString());

        } catch (CosmosException ex) {
            return handleCreateException(ex.getStatusCode(), ex.getMessage(), houseDAO);
        } catch (WebApplicationException ex) {
            return handleCreateException(ex.getResponse().getStatus(), ex.getMessage(), houseDAO);
        }
    }

    private Response handleCreateException(int statusCode, String msg, HouseDAO houseDAO) {
        if (msg.contains("House"))
            return processException(statusCode, HOUSE_MSG, houseDAO.getId());
        else
            return processException(statusCode, USER_MSG, houseDAO.getOwnerId());
    }

    private Response handleUpdateException(int statusCode, String msg, String id) {
        if (msg.contains("House"))
            return processException(statusCode, HOUSE_MSG, id);
        else
            return processException(statusCode, msg, id);
    }

    @Override
    public Response deleteHouse(String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try {
            //TODO: Quando se elimina um User, colocar nas casas (Deleted User)
            var item = db.getById(id, CONTAINER, HouseDAO.class).stream().findFirst();
            String ownerId = null;
            if (item.isPresent())
                ownerId = item.get().getOwnerId();
            else
                return sendResponse(NOT_FOUND, HOUSE_MSG, id);

            db.deleteHouse(id);

            var user = db.getById(ownerId, UsersResource.CONTAINER, UserDAO.class).stream().findFirst();
            if (user.isPresent()) {
                var updatedUser = user.get();
                updatedUser.removeHouse(id);
                db.updateUser(updatedUser);
                try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                    Cache.deleteFromCache(HOUSE_PREFIX, id, jedis);
                    Cache.putInCache(updatedUser, UsersService.USER_PREFIX, jedis);
                }
            }

        } catch (CosmosException ex) {
            throw new Exception("Error " + ex.getStatusCode() + " " + ex.getMessage());
            // return handleDeleteException(ex.getStatusCode(), ex.getMessage(), id/*, ownerId*/); // adicionar aqui o ownerId quando tivermos as
            // cookies
        }

        return sendResponse(OK, String.format("House %s was deleted", id));
    }

    @Override
    public Response getHouse(String id) throws Exception {
        if (badParams(id))
            return sendResponse(BAD_REQUEST, BAD_REQUEST_MSG);

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {

            String house = Cache.getFromCache(HOUSE_PREFIX, id, jedis);
            if (house != null)
                return sendResponse(OK, mapper.readValue(house, HouseDAO.class).toHouse());

            var res = db.getById(id, CONTAINER, HouseDAO.class).stream().findFirst();
            if (res.isPresent()) {
                var houseToCACHE = res.get();
                Cache.putInCache(houseToCACHE, HOUSE_PREFIX, jedis);
                return sendResponse(OK, houseToCACHE.toHouse());
            } else
                return sendResponse(NOT_FOUND, HOUSE_MSG, id);
        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), ex.getMessage());
        }
    }

    @Override
    public Response updateHouse(String id, House house) throws Exception {
        try {
            var updatedHouse = genUpdatedHouse(id, house);
            db.updateHouse(updatedHouse);

            try (Jedis jedis = RedisCache.getCachePool().getResource()) {
                Cache.putInCache(updatedHouse, HOUSE_PREFIX, jedis);
            }

            return sendResponse(OK, updatedHouse.toHouse());

        } catch (CosmosException ex) {
            return handleUpdateException(ex.getStatusCode(), ex.getMessage(), id);
        } catch (WebApplicationException ex) {
            return handleUpdateException(ex.getResponse().getStatus(), ex.getMessage(), id);
        }
    }

    // TODO: Quando fazemos RENTAL
    // 1 - Write na DB
    // 2.1 - Get do resultado? - se o rental que nós fizemos ficar na DB então podemos enviar a resposta ao cliente de sucesso
    // 2.2 - Se não ficar, enviar msg a dizer que já está alugado
    @Override
    public Response getAvailHouseByLocation(String location) throws Exception {
        //TODO: chache

        List<House> result = new ArrayList<>();

        try {
            var houses = db.getHousesByLocation(location).stream().toList();

            // Check if house is available
            // TODO: há uma forma melhor de fazer isto

            for (HouseDAO h : houses) {
                boolean available = true;
                for (String rentalId : h.getRentalsIds()) {
                    var rental = db.getRentalById(h.getId(), rentalId).stream().findFirst();
                    if (rental.isPresent() && rental.get().getEndDate().before(Date.from(Instant.now())))
                        available = false;
                }
                if (available)
                    result.add(h.toHouse());
            }
        } catch (CosmosException ex) {
            return processException(ex.getStatusCode(), "Mudar este nome");
        }

        return sendResponse(OK, result);
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
        Date iniDate = Date.from(Instant.parse(initialDate));
        Date eDate = Date.from(Instant.parse(endDate));

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
                    available = r.getInitialDate().after(eDate) || r.getEndDate().before(iniDate);
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
     * Returns updated houseDAO to the method who's making the request to the database
     *
     * @param id    of the house being accessed
     * @param house new house attributes
     * @return updated userDAO to the method who's making the request to the database
     * @throws Exception If id is null or if the user does not exist
     */
    private HouseDAO genUpdatedHouse(String id, House house) throws Exception {
        if (badParams(id))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);

        var res = db.getById(id, CONTAINER, HouseDAO.class).stream().findFirst();
        if (res.isPresent()) {
            HouseDAO houseDAO = res.get();

            String newName = house.getName();
            if (!newName.isBlank())
                houseDAO.setName(newName);

            String newLocation = house.getLocation();
            if (!newLocation.isBlank())
                houseDAO.setLocation(newLocation);

            var newPhotos = house.getPhotosIds();
            MediaResource media = new MediaResource();
            if (!newPhotos.isEmpty())
                if (!media.hasPhotos(newPhotos))
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                else
                    houseDAO.setPhotosIds(newPhotos);

            String newDescription = house.getDescription();
            if (!newDescription.isBlank())
                houseDAO.setDescription(newDescription);

            var newPrice = house.getPrice();
            if (newPrice != null)
                if (newPrice > 0)
                    houseDAO.setPrice(newPrice);
                else
                    throw new WebApplicationException("Error: Invalid price", Response.Status.BAD_REQUEST);

            var newDiscount = house.getDiscount();
            if (newDiscount != null)
                if (newDiscount >= 0)
                    houseDAO.setDiscount(newDiscount);
                else
                    throw new WebApplicationException("Error: Invalid discount", Response.Status.BAD_REQUEST);

            return houseDAO;

        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    private UserDAO checksHouseCreation(HouseDAO houseDAO, Jedis jedis) {
        if (badParams(houseDAO.getId(), houseDAO.getName(), houseDAO.getLocation(), houseDAO.getPrice().toString(),
                houseDAO.getDiscount().toString()) && houseDAO.getPrice() > 0 && houseDAO.getDiscount() > 0) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        /*// Check if house already exists on Cache
        if (jedis.get(HousesService.HOUSE_PREFIX + houseDAO.getId()) != null)
            throw new WebApplicationException("House", Response.Status.CONFLICT);*/

        // Check if house already exists on DB
        var house = db.getById(houseDAO.getId(), CONTAINER, HouseDAO.class).stream().findFirst();
        if (house.isPresent())
            throw new WebApplicationException("House", Response.Status.CONFLICT);

        MediaResource media = new MediaResource();
        if (!media.hasPhotos(houseDAO.getPhotosIds()))
            throw new WebApplicationException(Response.Status.NOT_FOUND);

       /* // Checks if the user exists in cache
        UserDAO user = mapper.readValue(jedis.get(UsersService.USER_PREFIX + houseDAO.getOwnerId()), UserDAO.class);
        if (user != null)
            return user;*/

        // Checks if the user exists in DB
        var dbUser = db.getById(houseDAO.getOwnerId(), UsersResource.CONTAINER, UserDAO.class).stream().findFirst();
        if (dbUser.isEmpty())
            throw new WebApplicationException("User", Response.Status.NOT_FOUND);

        return dbUser.get();
    }


}
