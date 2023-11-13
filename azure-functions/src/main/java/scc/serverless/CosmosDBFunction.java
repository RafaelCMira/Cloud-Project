package scc.serverless;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import com.microsoft.azure.functions.*;
import scc.cache.Cache;
import scc.data.HouseDAO;

/**
 * Azure Functions with Timer Trigger.
 */
public class CosmosDBFunction {
	 static final String NEW_HOUSES_PREFIX = "newH:";
	 static final String DISCOUNT_HOUSES = "houses:disc:";
	 static final String DB_NAME = "scc24db60700";
	 static final String HOUSES_COLLECTION = "houses";
	 static final int EXPIRATION_TIME = 300;

	private static final ObjectMapper mapper = new ObjectMapper();
    @FunctionName("cosmosDBNewHouses")
    public void updateMostRecentHouses(@CosmosDBTrigger(name = "cosmosNewHouses",
    										databaseName = DB_NAME,
    										collectionName = HOUSES_COLLECTION,
    										preferredLocations="West Europe",
											createLeaseCollectionIfNotExists = true,
    										connectionStringSetting = "AzureCosmosDBConnection")
        							HouseDAO[] houses,
        							final ExecutionContext context ) {
		context.getLogger().info(houses.length + "house(s) is/are changed");
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			for( HouseDAO h : houses) {
				jedis.lpush(NEW_HOUSES_PREFIX, mapper.writeValueAsString(h));
			}
			jedis.ltrim(NEW_HOUSES_PREFIX, 0, 9);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@FunctionName("cosmosDBDiscountHouses")
	public void updateDiscountHouses(@CosmosDBTrigger(name = "cosmosDiscHouses",
			databaseName = DB_NAME,
			collectionName = HOUSES_COLLECTION,
			preferredLocations="West Europe",
			createLeaseCollectionIfNotExists = true,
			connectionStringSetting = "AzureCosmosDBConnection")
									   HouseDAO[] houses,
									   final ExecutionContext context ) {
		context.getLogger().info(houses.length + "house(s) is/are changed");
		try (Jedis jedis = Cache.getCachePool().getResource()) {
			for( HouseDAO h : houses) {
				if (h.getDiscount() > 0)
					jedis.lpush(DISCOUNT_HOUSES, mapper.writeValueAsString(h));
			}
			jedis.expire(DISCOUNT_HOUSES,EXPIRATION_TIME);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
