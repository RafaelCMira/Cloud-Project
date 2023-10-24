package scc.srv.users;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import scc.data.User;
import scc.data.UserDAO;

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

    String USER_PREFIX = "u:";

    String USER_HOUSES_PREFIX = "h:";

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createUser(UserDAO userDAO) throws Exception;

/*    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    String createUser(@FormDataParam("userDAO") UserDAO userDAO,
                      @FormDataParam("image") InputStream imageStream) throws Exception;*/

    @DELETE
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    String deleteUser(@PathParam(ID) String id) throws Exception;

    @GET
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    User getUser(@PathParam(ID) String id) throws Exception;

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    User updateUser(@PathParam(ID) String id, User user) throws Exception;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<User> listUsers() throws Exception;

    @GET
    @Path("/{" + ID + "}" + HOUSES)
    @Produces(MediaType.APPLICATION_JSON)
    List<String> getUserHouses(@PathParam(ID) String id) throws Exception;

}
