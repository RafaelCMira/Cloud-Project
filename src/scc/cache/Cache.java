package scc.cache;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.srv.utils.HasId;
import scc.utils.mgt.AzureManagement;
import scc.utils.props.AzureProperties;

public class Cache {
    private static final String RedisHostname = System.getenv(AzureProperties.REDIS_URL);
    private static final String RedisKey = System.getenv(AzureProperties.REDIS_KEY);

    private static JedisPool instance;

    public synchronized static JedisPool getCachePool() {
        if (instance != null)
            return instance;
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        instance = new JedisPool(poolConfig, RedisHostname, 6380, 1000, RedisKey, true);
        return instance;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public Cache() {
    }

    public static <T extends HasId> void putInCache(T obj, String prefix) throws JsonProcessingException {
        if (AzureManagement.CREATE_REDIS)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                jedis.set(prefix + obj.getId(), mapper.writeValueAsString(obj));
            }
    }

    public static void putInCache(BinaryData data, String prefix) throws JsonProcessingException {
        if (AzureManagement.CREATE_REDIS)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                jedis.set(prefix + data, mapper.writeValueAsString(data));
            }
    }

    public static String getFromCache(String prefix, String key) {
        if (AzureManagement.CREATE_REDIS)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                return jedis.get(prefix + key);
            }
        return null;
    }

    public static void deleteFromCache(String prefix, String id) {
        if (AzureManagement.CREATE_REDIS)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                jedis.del(prefix + id);
            }
    }
}
