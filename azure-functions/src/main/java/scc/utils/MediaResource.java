package scc.utils;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Resource for managing media files, such as images.
 */
public class MediaResource {

    //private final static String MAIN_STORAGE_CONNECTION = System.getenv("BlobStoreConnection");

    private final static String REPLICATED_STORAGE_CONNECTION = System.getenv("ReplicatedBlobStoreConnection");

    private final static String CONTAINER_NAME = "media";

    private final ObjectMapper mapper = new ObjectMapper();

    public String upload(byte[] contents) {
        
        String id = Hash.of(contents);
        try {
            BinaryData data = BinaryData.fromBytes(contents);

            // Get container client
            BlobContainerClient containerClient = getContainerClient();

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
            BlobContainerClient containerClient = getContainerClient();

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient(id);

            // Download contents to BinaryData (check documentation for other alternatives)
            BinaryData data = blob.downloadContent();
            content = data.toBytes();

            if (content == null)
                return Response.status(404).build();

        } catch (Exception ignored) {

        }

        return Response.ok(content).build();
    }

    public boolean hasPhotos(String id) {
        try {
            // Get container client
            BlobContainerClient containerClient = getContainerClient();
            if (!containerClient.getBlobClient(id).exists())
                return false;
        } catch (Exception ignored) {
        }
        return true;
    }

    // Get container client
    private BlobContainerClient getContainerClient() {
        return new BlobContainerClientBuilder()
                .connectionString(REPLICATED_STORAGE_CONNECTION)
                .containerName(CONTAINER_NAME)
                .buildClient();
    }
}
