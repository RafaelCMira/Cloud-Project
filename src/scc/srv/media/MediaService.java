package scc.srv.media;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scc.srv.authentication.Session;
import scc.utils.props.AzureProperties;

import java.util.List;

@Path(MediaService.PATH)
public interface MediaService {
    String PATH = "/media";

    
    String CONTAINER_NAME = "media";
    String MEDIA_PREFIX = "media:";


    String ID = "id";

    String storageConnectionString = System.getenv(AzureProperties.BLOB_KEY);


    /**
     * Post a new image. The id of the media file is its hash.
     *
     * @param contents bytes of the file
     * @return id of media file
     */
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    String upload(@CookieParam(Session.SESSION) Cookie session, byte[] contents);


    /**
     * Return the contents of media file. Throw an appropriate error message if
     * id does not exist.
     *
     * @param id id of media file
     * @return bytes of the file
     */
    @GET
    @Path("/{" + ID + "}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response download(@PathParam(ID) String id);


    /**
     * Lists the ids of media files
     *
     * @return list with all ids
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<String> listImages();
}
