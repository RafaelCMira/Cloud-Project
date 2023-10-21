package scc.srv.rentals;

import com.azure.core.annotation.Get;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import scc.data.*;

import java.util.List;

@Path(RentalService.PATH)

public interface RentalService {
    String PATH = "/house";
    String HOUSE_ID = "houseId";
    String RENTAL_ID = "id";
    String RENTAL = "/rental";

    String DISCOUNT = "discount";

    String CACHE_PREFIX = "rental:";


    @POST
    @Path("{" + HOUSE_ID + "}" + RENTAL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createRental(@PathParam(HOUSE_ID) String houseID, RentalDAO rentalDAO) throws Exception;

    @GET
    @Path("{" + HOUSE_ID + "}" + RENTAL + "{" + RENTAL_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Rental getRental(@PathParam(HOUSE_ID) String houseID, @PathParam(RENTAL_ID) String id) throws Exception;

    @PUT
    @Path("/{" + HOUSE_ID + "}" + RENTAL + "/{" + RENTAL_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Rental updateRental(@PathParam(HOUSE_ID) String houseID, @PathParam(RENTAL_ID) String id, RentalDAO rentalDAO) throws Exception;

    @GET
    @Path("/{" + HOUSE_ID + "}" + RENTAL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    List<Rental> listRentals(@PathParam(HOUSE_ID) String houseID);


    @DELETE
    @Path("/{" + HOUSE_ID + "}" + RENTAL + "/{" + RENTAL_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    String deleteRental(@PathParam(HOUSE_ID) String houseID, @PathParam(RENTAL_ID) String id) throws Exception;

    @GET
    @Path("/{" + HOUSE_ID + "}" + RENTAL + "/{" + DISCOUNT + "}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    List<Rental> getDiscountedRentals(@PathParam(HOUSE_ID) String houseID) throws Exception;

}
