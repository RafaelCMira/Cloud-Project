package scc.srv.rentals;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import scc.data.*;

import javax.print.attribute.standard.Media;
import java.util.List;

@Path(RentalService.PATH)

public interface RentalService {
    String PATH = "/house";
    String RENTAL_ID = "id";
    String RENTAL = "/rental";


    @POST
    @Path(RENTAL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createRental(RentalDAO rentalDAO) throws Exception;

    @GET
    @Path("{" + RENTAL_ID + "}" + RENTAL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Rental getRental(@PathParam(RENTAL_ID) String id) throws Exception;

    @PUT
    @Path("/{" + RENTAL_ID + "}" + RENTAL)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Rental updateRental(@PathParam(RENTAL_ID) String id, RentalDAO rentalDAO) throws Exception;
}
