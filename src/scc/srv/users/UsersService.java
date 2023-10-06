package scc.srv.users;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.User;
import scc.data.UserDAO;


@Path(UsersService.PATH)
public interface UsersService {
    String PATH = "/users";
    String ID = "id"; // nickname
    String NAME = "name";
    String PWD = "pwd";
    String PHOTO_ID = "photoId";
    String HOUSE_IDS = "houseIds";
    String QUERY = "query";


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createUser(UserDAO userDAO) throws Exception;

    @DELETE
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    User deleteUser(@PathParam(ID) String id) throws Exception;

    @GET
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    User getUser(@PathParam(ID) String id) throws Exception;

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    User updateUser(@PathParam(ID) String id, UserDAO userDAO) throws Exception;


}
