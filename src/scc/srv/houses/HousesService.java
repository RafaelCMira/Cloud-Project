package scc.srv.houses;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.data.HouseDAO;
import scc.data.House;

import java.util.List;

@Path(HousesService.PATH)
public interface HousesService {
    String PATH = "/house";
    String ID = "id";
    String LOCATION = "location";
    String INITIAL_DATE = "initialDate";
    String END_DATE = "endDate";

    String HOUSE_PREFIX = "house:";


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createHouse(HouseDAO houseDAO) throws Exception;

    @DELETE
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    String deleteHouse(@PathParam(ID) String id) throws Exception;

    @GET
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    House getHouse(@PathParam(ID) String id) throws Exception;

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    House updateHouse(@PathParam(ID) String id, House house) throws Exception;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    List<House> getAvailHouseByLocation(@QueryParam(LOCATION) String location) throws Exception;

    @GET
    @Path("/Available/")
    @Produces(MediaType.APPLICATION_JSON)
    List<House> getHouseByLocationPeriod(@QueryParam(LOCATION) String location, @QueryParam(INITIAL_DATE) String initialDate,
                                         @QueryParam(END_DATE) String endDate) throws Exception;
}
