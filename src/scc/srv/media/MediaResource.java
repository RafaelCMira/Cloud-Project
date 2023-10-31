package scc.srv.media;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import scc.cache.Cache;
import scc.utils.Hash;
import scc.utils.mgt.AzureManagement;

import java.util.ArrayList;
import java.util.List;

/**
 * Resource for managing media files, such as images.
 */
public class MediaResource implements MediaService {
    private final ObjectMapper mapper = new ObjectMapper();

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

            if (AzureManagement.CREATE_REDIS) {
                Cache.putInCache(data, MEDIA_PREFIX);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public byte[] download(String id) {
        byte[] content = null;

        try {

            if (AzureManagement.CREATE_REDIS) {
                var res = Cache.getFromCache(MEDIA_PREFIX, id);
                if (res != null)
                    return mapper.readValue(res, BinaryData.class).toBytes();
            }

            // Get container client
            BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient(id);

            // Download contents to BinaryData (check documentation for other alternatives)
            BinaryData data = blob.downloadContent();
            content = data.toBytes();
            if (content == null)
                throw new Exception(String.format("Id: %s does not exist", id));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
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
