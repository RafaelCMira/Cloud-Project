package scc.srv.utils;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

/**
 * Class with generic methods to use across resources, cache for example
 */
public class Cache {

    private static final ObjectMapper mapper = new ObjectMapper();

    public Cache() {
    }

    public static <T extends HasId> void putInCache(T obj, String prefix, Jedis jedis) throws JsonProcessingException {
        jedis.set(prefix + obj.getId(), mapper.writeValueAsString(obj));
    }

    public static void putInCache(BinaryData data, String prefix, Jedis jedis) throws JsonProcessingException {
        jedis.set(prefix + data, mapper.writeValueAsString(data));
    }

    public static String getFromCache(String prefix, String key, Jedis jedis) {
        return jedis.get(prefix + key);
    }

    public static void deleteFromCache(String prefix, String id, Jedis jedis) {
        jedis.del(prefix + id);
    }
}
