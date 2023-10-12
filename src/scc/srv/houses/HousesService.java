package scc.srv.houses;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.HouseDAO;
import scc.data.House;

@Path(HousesService.PATH)
public interface HousesService {
    String PATH = "/house";
    String ID = "id";
    String NAME = "name";
    String LOCATION = "location";
    String DESCRIPTION = "description";
    String PHOTO_ID = "photoId";
    String OWNER_ID = "ownerID";
    String QUERY = "query";


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
}
