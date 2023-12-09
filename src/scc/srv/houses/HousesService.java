package scc.srv.houses;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.data.HouseDAO;
import scc.data.House;
import scc.srv.authentication.Session;

@Path(HousesService.PATH)
public interface HousesService {
    String PATH = "/house";


    String PARTITION_KEY = "/id";
    String COLLECTION = "houses";
    String HOUSE_PREFIX = "h:";

    String MOST_RECENT_DISCOUNTS = "houses:disc";

    String HOUSES_BY_LOCATION_PREFIX = "h:location:%s-off:%s";

    String NEW_HOUSES_PREFIX = "newH:";

    String ID = "id";

    String QUESTION = "question";
    String DESCRIPTION = "description";
    String LOCATION = "location";
    String ALL = "/all";
    String AVAILABLE = "/available";
    String NEW_HOUSES = "/newHouses";
    String HOUSE_BY_DESC = "/housesByDescription";

    String HOUSE_BY_QUESTION = "/housesByQuestion";

    String COGNITIVE_SEARCH = "/cognitiveSearch";
    String INITIAL_DATE = "initialDate";
    String END_DATE = "endDate";

    String OFFSET = "offset";


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createHouse(@CookieParam(Session.SESSION) Cookie session, HouseDAO houseDAO) throws Exception;

    @DELETE
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    Response deleteHouse(@CookieParam(Session.SESSION) Cookie session, @PathParam(ID) String id) throws Exception;

    @GET
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getHouse(@PathParam(ID) String id) throws Exception;

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response updateHouse(@CookieParam(Session.SESSION) Cookie session, @PathParam(ID) String id, House house) throws Exception;

    @GET
    @Path(ALL)
    @Produces(MediaType.APPLICATION_JSON)
    Response listAllHouses();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response getAvailableHouseByLocation(@QueryParam(LOCATION) String location, @QueryParam(OFFSET) int offset);

    @GET
    @Path(AVAILABLE)
    @Produces(MediaType.APPLICATION_JSON)
    Response getHouseByLocationPeriod(@QueryParam(LOCATION) String location, @QueryParam(INITIAL_DATE) String initialDate,
                                      @QueryParam(END_DATE) String endDate, @QueryParam(OFFSET) int offset);

    @GET
    @Path(NEW_HOUSES)
    @Produces(MediaType.APPLICATION_JSON)
    Response getNewHouses();

}
