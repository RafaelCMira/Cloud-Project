package scc.serverless;

import com.microsoft.azure.functions.annotation.*;

import com.microsoft.azure.functions.*;
import scc.utils.MediaResource;


/**
 * Azure Functions with Blob Trigger.
 */
public class BlobStorageFunction {

    private final MediaResource media = new MediaResource();
    private static final String PATH = "media/{name}";

    @FunctionName("replicateBlob")
    public void replicateBlob(
            @BlobTrigger(name = "replicateBlob",
                    dataType = "binary",
                    path = PATH,
                    connection = "BlobStoreConnection") byte[] content,
            @BindingName("name") String blobname,
            final ExecutionContext context)
    {
        if(!media.hasPhotos(blobname))
            media.upload(content);
    }
}