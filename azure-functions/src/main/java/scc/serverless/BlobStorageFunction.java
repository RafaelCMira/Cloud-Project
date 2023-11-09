package scc.serverless;

import com.microsoft.azure.functions.annotation.*;

import com.microsoft.azure.functions.*;
import scc.utils.Hash;
import scc.utils.MediaResource;


/**
 * Azure Functions with Blob Trigger.
 */
public class BlobStorageFunction {

    private final MediaResource media = new MediaResource();

    @FunctionName("replicateBlob")
    public void replicateBlob(
            @BlobTrigger(name = "replicateBlob",
                    dataType = "binary",
                    path = "media/{name}",
                    connection = "BlobStoreConnection") byte[] content,
            @BindingName("name") String blobname,
            final ExecutionContext context)
    {
        context.getLogger().info(blobname + ": blob changed");

        if(!media.hasPhotos(blobname))
            media.upload(content);
    }
}