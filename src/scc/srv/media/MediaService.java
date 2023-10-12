package scc.srv.media;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path(MediaService.PATH)
public interface MediaService {

    String PATH = "/media";

    // Get connection string in the storage access keys page
    String storageConnectionString = System.getenv("BLOB_KEY");

    String CONTAINER_NAME = "images";


    /**
     * Post a new image. The id of the media file is its hash.
     *
     * @param contents bytes of the file
     * @return id of media file
     */
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    String upload(byte[] contents);


    /**
     * Return the contents of media file. Throw an appropriate error message if
     * id does not exist.
     *
     * @param id id of media file
     * @return bytes of the file
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    byte[] download(@PathParam("id") String id);


    /**
     * Lists the ids of media files
     *
     * @return list with all ids
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<String> list();
}
