package scc.srv.users;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import scc.data.User;
import scc.data.UserDAO;
import scc.srv.utils.Login;

import java.io.InputStream;
import java.util.List;


@Path(UsersService.PATH)
public interface UsersService {
    String PATH = "/user";
    String ID = "id"; // nickname
    String NAME = "name";
    String PWD = "pwd";
    String PHOTO_ID = "photoId";
    String HOUSES = "/houses";
    String QUERY = "query";

    String AUTH = "/auth";

    String USER_PREFIX = "u:";

    String USER_HOUSES_PREFIX = "h:";

    @POST
    @Path(AUTH)
    @Consumes(MediaType.APPLICATION_JSON)
    Response authUser(Login user) throws Exception;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createUser(UserDAO userDAO) throws Exception;

    @DELETE
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    Response deleteUser(@PathParam(ID) String id) throws Exception;

    @GET
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getUser(@PathParam(ID) String id) throws Exception;

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response updateUser(@PathParam(ID) String id, User user) throws Exception;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response listUsers() throws Exception;

    @GET
    @Path("/{" + ID + "}" + HOUSES)
    @Produces(MediaType.APPLICATION_JSON)
    Response getUserHouses(@PathParam(ID) String id) throws Exception;

}

