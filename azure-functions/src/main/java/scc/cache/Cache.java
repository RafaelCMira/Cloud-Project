package scc.cache;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;

public class Cache {
    private static final String RedisHostname = System.getenv("REDIS_URL");
    private static final String RedisKey = System.getenv("REDIS_KEY");

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
}
