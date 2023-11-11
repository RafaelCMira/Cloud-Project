package scc.srv.media;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import scc.cache.Cache;
import scc.utils.Hash;

import java.util.ArrayList;
import java.util.List;

import static scc.srv.utils.Utility.*;

/**
 * Resource for managing media files, such as images.
 */
public class MediaResource implements MediaService {

    public String upload(byte[] contents) {

        String id = Hash.of(contents);
        try {
            BinaryData data = BinaryData.fromBytes(contents);

            // Get container client
            BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient(id);

            // Upload contents from BinaryData (check documentation for other alternatives)
            blob.upload(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public Response download(String id) {
        byte[] content = null;

        try {
            // Get container client
            BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient(id);

            // Download contents to BinaryData (check documentation for other alternatives)
            BinaryData data = blob.downloadContent();
            content = data.toBytes();

            if (content == null)
                return sendResponse(NOT_FOUND, MEDIA_MSG, id);

        } catch (Exception ignored) {

        }

        return Response.ok(content).build();
    }


    public boolean hasPhotos(List<String> photosIds) {
        try {
            // Get container client
            BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);
            for (String photo : photosIds)
                if (!containerClient.getBlobClient(photo).exists())
                    return false;
        } catch (Exception ignored) {
        }
        return true;
    }

    public List<String> listImages() {
        // Get container client
        BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);

        //Get blobs
        var blobs = containerClient.listBlobs();

        // List blobs in the container
        List<String> blobNames = new ArrayList<>();
        for (BlobItem blobItem : blobs)
            blobNames.add(blobItem.getName());

        return blobNames;
    }

    // Get container client
    private BlobContainerClient getContainerClient(String containerName) {
        return new BlobContainerClientBuilder()
                .connectionString(MediaService.storageConnectionString)
                .containerName(containerName)
                .buildClient();
    }
}
