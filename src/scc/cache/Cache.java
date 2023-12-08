package scc.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.srv.utils.HasId;
import scc.utils.props.AzureProperties;

import java.util.ArrayList;
import java.util.List;

public class Cache {
    private static final long CACHE_EXPIRE_TIME = 300; // 5 minutes
    private static final String RedisHostname = System.getenv("REDIS");
    // private static final String RedisKey = System.getenv(AzureProperties.REDIS_KEY);
    private static final boolean CACHE_ON = false;

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
        instance = new JedisPool(poolConfig, RedisHostname, 6379, 1000, false);
        return instance;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public Cache() {
    }

    public static <T extends HasId> void putCookieInCache(T obj, String prefix) throws JsonProcessingException {
        try (Jedis jedis = Cache.getCachePool().getResource()) {
            jedis.set(prefix + obj.getId(), mapper.writeValueAsString(obj));
        }
    }

    public static String getCookieFromCache(String prefix, String key) {
        try (Jedis jedis = Cache.getCachePool().getResource()) {
            return jedis.get(prefix + key);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T extends HasId> void putInCache(T obj, String prefix) throws JsonProcessingException {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                jedis.set(prefix + obj.getId(), mapper.writeValueAsString(obj));
            }
    }

    public static String getFromCache(String prefix, String key) {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                return jedis.get(prefix + key);
            }
        return null;
    }

    public static void deleteFromCache(String prefix, String id) {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                jedis.del(prefix + id);
            }
    }

    public static void deleteAllFromCache(String prefix, List<String> ids) {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                for (String id : ids)
                    jedis.del(prefix + id);
            }
    }

    public static List<String> getListFromCache(String key) {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                return jedis.lrange(key, 0, -1);
            }
        return new ArrayList<>();
    }

    public static <T extends HasId> void putListInCache(List<T> list, String key) throws JsonProcessingException {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                for (T obj : list) {
                    jedis.lpush(key, mapper.writeValueAsString(obj));
                }
                jedis.expire(key, CACHE_EXPIRE_TIME);
            }
    }

    public static <T> void addToListInCache(T obj, String key) throws JsonProcessingException {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                jedis.lpush(key, mapper.writeValueAsString(obj));
                jedis.expire(key, CACHE_EXPIRE_TIME);
            }
    }

    public static <T> void removeFromListInCache(T obj, String key) throws JsonProcessingException {
        if (CACHE_ON) {
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                String valueToRemove = mapper.writeValueAsString(obj);
                jedis.lrem(key, 0, valueToRemove);
            }
        }
    }

    public static void removeListInCache(String key) {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                jedis.ltrim(key, 0, 0);
            }
    }

    public static boolean hasKey(String key) {
        if (CACHE_ON)
            try (Jedis jedis = Cache.getCachePool().getResource()) {
                var res = jedis.get(key);
                if (res != null && !res.isEmpty())
                    return true;
            }
        return false;
    }


}
