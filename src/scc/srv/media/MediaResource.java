package scc.srv.media;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.srv.utils.Cache;
import scc.utils.Hash;

import java.util.ArrayList;
import java.util.List;

/**
 * Resource for managing media files, such as images.
 */
public class MediaResource implements MediaService {
    private final ObjectMapper mapper = new ObjectMapper();

    public String upload(byte[] contents) {
        String id = Hash.of(contents);
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            BinaryData data = BinaryData.fromBytes(contents);

            // Get container client
            BlobContainerClient containerClient = getContainerClient(MediaService.CONTAINER_NAME);

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient(id);

            // Upload contents from BinaryData (check documentation for other alternatives)
            blob.upload(data);

            Cache.putInCache(data, MEDIA_PREFIX);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public byte[] download(String id) {
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
