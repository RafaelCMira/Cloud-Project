package scc.serverless;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import com.microsoft.azure.functions.*;
import scc.data.HouseDAO;

/**
 * Azure Functions with Timer Trigger.
 */
public class CosmosDBFunction {

	//private static final String AzureCosmosDBConnection =  System.getenv("AzureCosmosDBConnection");
	//private static final String COSMOSDB_DATABASE =  System.getenv("COSMOSDB_DATABASE");

	private static final ObjectMapper mapper = new ObjectMapper();
    @FunctionName("cosmosDBNewHouses")
    public void updateMostRecentHouses(@CosmosDBTrigger(name = "cosmosNewHouses",
    										databaseName = "scc24db60700",
    										collectionName = "houses",
    										preferredLocations="West Europe",
    										connectionStringSetting = "AzureCosmosDBConnection")
        							HouseDAO[] houses,
        							final ExecutionContext context ) {
		context.getLogger().info(houses.length + "house(s) is/are changed");
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			for( HouseDAO h : houses) {
				jedis.lpush("newH:", mapper.writeValueAsString(h));
			}
			jedis.ltrim("newH:", 0, 9);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
