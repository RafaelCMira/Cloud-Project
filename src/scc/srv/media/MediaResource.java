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
            // BinaryData data = BinaryData.fromBytes(contents);

            // Get container client
            // BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);

            // Get client to blob
            // BlobClient blob = containerClient.getBlobClient(id);

            // Upload contents from BinaryData (check documentation for other alternatives)
            // blob.upload(data);

            Files.write(path,contents);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public Response download(String id) {
        byte[] content = null;

        try {
            // Get container client
            //BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);

            // Get client to blob
            //BlobClient blob = containerClient.getBlobClient(id);

            // Download contents to BinaryData (check documentation for other alternatives)
            //BinaryData data = blob.downloadContent();
            Path path = Path.of(STORAGE_PATH + id);
            if (!Files.exists(path))
                return sendResponse(NOT_FOUND, MEDIA_MSG, id);

            content = Files.readAllBytes(path);

            // content = data.toBytes();

            if (content.length == 0)
                return sendResponse(NOT_FOUND, MEDIA_MSG, id);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Response.ok(content).build();
    }


    public boolean hasPhotos(List<String> photosIds) {
        try {
            // Get container client
            //BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);
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
        // Get container client
        //BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);

        //Get blobs
        Path path = Path.of(STORAGE_PATH);
        File f = new File(path.toUri());

        var blob = f.list();
        if (blob == null) {
            return new ArrayList<>();
        }

        // List blobs in the container
        return Arrays.stream(blob).toList();
    }

    /*
    // Get container client
    private BlobContainerClient getContainerClient(String containerName) {
        return new BlobContainerClientBuilder()
                .connectionString(MediaService.storageConnectionString)
                .containerName(containerName)
                .buildClient();
    }
    */
}
