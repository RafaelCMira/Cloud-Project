package scc.srv.houses;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.HouseDAO;
import scc.data.House;

import java.time.LocalDate;
import java.util.List;

@Path(HousesService.PATH)
public interface HousesService {
    String PATH = "/house";
    String ID = "id";
    String LOCATION = "location";
    String INITIAL_DATE = "initialDate";
    String END_DATE = "endDate";

    String CACHE_PREFIX = "house:";


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createHouse(HouseDAO houseDAO) throws Exception;

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
    House updateHouse(@PathParam(ID) String id, HouseDAO houseDAO) throws Exception;

   /* @GET
    @Path("/{" + LOCATION + "}")
    @Produces(MediaType.APPLICATION_JSON)
    List<House> getAvailHouseByLocation(@PathParam(LOCATION) String location) throws Exception;

    @GET
    @Path("/{" + LOCATION + "}")
    @Produces(MediaType.APPLICATION_JSON)
    List<House> getHouseByLocationPeriod(@PathParam(LOCATION) String location,@QueryParam(INITIAL_DATE) LocalDate initialDate,
                                         @QueryParam(END_DATE) LocalDate endDate) throws Exception;*/
}
