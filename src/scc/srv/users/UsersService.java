package scc.srv.users;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.data.User;
import scc.data.UserDAO;
import scc.srv.authentication.Login;
import scc.srv.authentication.Session;


@Path(UsersService.PATH)
public interface UsersService {
    String PATH = "/user";


    String PARTITION_KEY = "/id";
    String COLLECTION = "users";
    String USER_PREFIX = "u:";

    String USER_HOUSES_PREFIX = "u:%s-h:-off:%s";

    String USER_RENTALS_PREFIX = "u:%s-r:-off:%s";


    String ID = "id";
    String HOUSES = "/houses";
    String AUTH = "/auth";
    String DELETED_USER = "Deleted User";

    String RENTALS = "/rentals";

    String OFFSET = "offset";


    @POST
    @Path(AUTH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response auth(Login credentials) throws Exception;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createUser(UserDAO userDAO) throws Exception;

    @DELETE
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    Response deleteUser(@CookieParam(Session.SESSION) Cookie session, @PathParam(ID) String id) throws Exception;

    @GET
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getUser(@PathParam(ID) String id) throws Exception;

    @PUT
    @Path("/{" + ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response updateUser(@CookieParam(Session.SESSION) Cookie session, @PathParam(ID) String id, User user) throws Exception;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response listUsers() throws Exception;

    @GET
    @Path("/{" + ID + "}" + HOUSES)
    @Produces(MediaType.APPLICATION_JSON)
    Response getUserHouses(@PathParam(ID) String id, @QueryParam(OFFSET) int offset) throws Exception;

    @GET
    @Path("/{" + ID + "}" + RENTALS)
    @Produces(MediaType.APPLICATION_JSON)
    Response getUserRentals(@PathParam(ID) String id, @QueryParam(OFFSET) int offset);

}

