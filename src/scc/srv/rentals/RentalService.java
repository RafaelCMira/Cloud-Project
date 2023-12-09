package scc.srv.rentals;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.data.*;
import scc.srv.authentication.Session;


@Path(RentalService.PATH)

public interface RentalService {
    String PATH = "/house";


    String PARTITION_KEY = "/houseId";
    String COLLECTION = "rentals";
    String RENTAL_PREFIX = "r:";


    String HOUSE_ID = "houseId";
    String RENTAL_ID = "id";
    String RENTAL = "/rental";
    String DISCOUNT = "/discount";

    String HOUSE_RENTALS = "r:house:%s-off:%s";
    String DISCOUNTED_HOUSES = "h:discount-off:%s";


    String OFFSET = "offset";


    @POST
    @Path("{" + HOUSE_ID + "}" + RENTAL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createRental(@CookieParam(Session.SESSION) Cookie session, @PathParam(HOUSE_ID) String houseId, RentalDAO rentalDAO) throws Exception;

    @GET
    @Path("{" + HOUSE_ID + "}" + RENTAL + "/{" + RENTAL_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getRental(@PathParam(HOUSE_ID) String houseID, @PathParam(RENTAL_ID) String id) throws Exception;

    @DELETE
    @Path("/{" + HOUSE_ID + "}" + RENTAL + "/{" + RENTAL_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response deleteRental(@PathParam(HOUSE_ID) String houseID, @PathParam(RENTAL_ID) String id);

    @PUT
    @Path("/{" + HOUSE_ID + "}" + RENTAL + "/{" + RENTAL_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response updateRental(@CookieParam(Session.SESSION) Cookie session, @PathParam(HOUSE_ID) String houseID, @PathParam(RENTAL_ID) String id, RentalDAO rentalDAO) throws Exception;

    @GET
    @Path("/{" + HOUSE_ID + "}" + RENTAL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response listHouseRentals(@PathParam(HOUSE_ID) String houseID, @QueryParam(OFFSET) int offset);

    @GET
    @Path(DISCOUNT)
    @Produces(MediaType.APPLICATION_JSON)
    Response getHousesInDiscount(@QueryParam(OFFSET) String offset);

}
