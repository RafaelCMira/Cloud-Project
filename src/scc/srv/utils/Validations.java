package scc.srv.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.data.HouseDAO;
import scc.data.UserDAO;
import scc.db.CosmosDBLayer;
import scc.srv.houses.HousesResource;
import scc.srv.houses.HousesService;
import scc.srv.media.MediaResource;
import scc.srv.users.UsersResource;
import scc.srv.users.UsersService;

import java.util.List;

import static scc.srv.utils.Utility.HOUSE_MSG;
import static scc.srv.utils.Utility.USER_MSG;

public class Validations {

    private static final CosmosDBLayer db = CosmosDBLayer.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();

    public Validations() {
    }

    /**
     * Verify if house exists
     */
    public static boolean houseExists(String houseId) {
        // Verify if house exists
        if (Cache.getFromCache(HousesService.HOUSE_PREFIX, houseId) == null) {
            var houseRes = db.getById(houseId, HousesResource.CONTAINER, HouseDAO.class).stream().findFirst();
            return houseRes.isPresent();
        }

        return true;
    }

    /**
     * Verify if user exists
     */
    public static boolean userExists(String userId) {
        if (Cache.getFromCache(UsersService.USER_PREFIX, userId) == null) {
            var userRes = db.getById(userId, UsersResource.CONTAINER, UserDAO.class).stream().findFirst();
            return userRes.isPresent();
        }

        return true;
    }

    /**
     * Verify if media exists
     */
    public static boolean mediaExists(List<String> mediaId) {
        MediaResource media = new MediaResource();
        return media.hasPhotos(mediaId) && mediaId != null && !mediaId.isEmpty();
    }

}
