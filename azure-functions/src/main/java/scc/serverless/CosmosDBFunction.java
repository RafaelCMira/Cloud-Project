package scc.serverless;

import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Timer Trigger.
 */
public class CosmosDBFunction {

	//private static final String AzureCosmosDBConnection =  System.getenv("AzureCosmosDBConnection");
	//private static final String COSMOSDB_DATABASE =  System.getenv("COSMOSDB_DATABASE");
    @FunctionName("cosmosDBNewHouses")
    public void updateMostRecentHouses(@CosmosDBTrigger(name = "cosmosNewHouses",
    										databaseName = "scc24db60700",
    										collectionName = "houses",
    										preferredLocations="West Europe",
    										createLeaseCollectionIfNotExists = true,
    										connectionStringSetting = "AzureCosmosDBConnection")
        							String[] houses,
        							final ExecutionContext context ) {
		context.getLogger().info(houses.length + "house(s) is/are changed");
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			for( String h : houses) {
				jedis.lpush("h:", h);
			}
			jedis.ltrim("houses:", 0, 9);
		}
    }

}
