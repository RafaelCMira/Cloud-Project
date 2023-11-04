package scc.srv.houses;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.data.HouseDAO;
import scc.data.House;
import scc.srv.authentication.Session;

import java.util.List;

@Path(HousesService.PATH)
public interface HousesService {
    String PATH = "/house";


    String PARTITION_KEY = "/id";
    String CONTAINER = "houses";
    String HOUSE_PREFIX = "h:";


    String ID = "id";
    String LOCATION = "location";
    String NEW_HOUSES = "/NewHouses";
    String INITIAL_DATE = "initialDate";
    String END_DATE = "endDate";


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
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    Response getAvailHouseByLocation(@QueryParam(LOCATION) String location);


    @GET
    @Path("/All")
    @Produces(MediaType.APPLICATION_JSON)
    Response listAllHouses();


    @GET
    @Path("/Available/")
    @Produces(MediaType.APPLICATION_JSON)
    List<House> getHouseByLocationPeriod(@QueryParam(LOCATION) String location, @QueryParam(INITIAL_DATE) String initialDate,
                                         @QueryParam(END_DATE) String endDate) throws Exception;

    @GET
    @Path(NEW_HOUSES)
    @Produces(MediaType.APPLICATION_JSON)
    List<String> getNewHouses() throws Exception;

}
