package scc.srv.media;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Null;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.utils.Hash;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static scc.srv.utils.Utility.*;

/**
 * Resource for managing media files, such as images.
 */
public class MediaResource implements MediaService {

    private static final String STORAGE_PATH = System.getenv("STORAGE_PATH");

    public String upload(byte[] contents) {

        String id = Hash.of(contents);
        Path path = Path.of(STORAGE_PATH + id);
        try {

            Files.write(path, contents);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public Response download(String id) {
        byte[] content = null;

        try {

            Path path = Path.of(STORAGE_PATH + id);
            if (!Files.exists(path))
                return sendResponse(NOT_FOUND, MEDIA_MSG, id);

            content = Files.readAllBytes(path);

            if (content.length == 0)
                return sendResponse(NOT_FOUND, MEDIA_MSG, id);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Response.ok(content).build();
    }

    public boolean hasPhotos(List<String> photosIds) {
        try {
            for (String photo : photosIds) {
                Path path = Path.of(STORAGE_PATH + photo);
                if (!Files.exists(path))
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public List<String> listImages() {

        //Get blobs
        Path path = Path.of(STORAGE_PATH);
        File f = new File(path.toUri());

        var blob = f.list();
        if (blob == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(blob).toList();
    }
    
}
