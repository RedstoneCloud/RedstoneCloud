package redis.cache;

import com.google.common.base.Preconditions;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collection;
import java.util.List;

@Getter
public class Cache {
    protected JedisPool pool;

    public Cache() {
        this.pool = createJedisPool();
    }

    protected JedisPool createJedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(16);
        config.setMaxIdle(64);
        config.setMaxTotal(256);
        config.setBlockWhenExhausted(true);
        return new JedisPool(config);
    }

    public String set(String key, String value) {
        try (Jedis jedis = this.pool.getResource()) {
            return jedis.set(key, value);
        }
    }

    public void setList(String key, Collection<String> collection) {
        Preconditions.checkArgument(!collection.isEmpty(), "List cannot be empty");
        try (Jedis jedis = this.pool.getResource()) {
            jedis.del(key);

            for (String element : collection) {
                jedis.rpush(key, element);
            }
        }
    }

    public String setMany(String... keysValues) {
        Preconditions.checkArgument(keysValues.length % 2 == 0, "Each key must have a value");
        try (Jedis jedis = this.pool.getResource()) {
            return jedis.mset(keysValues);
        }
    }

    public String get(String key) {
        try (Jedis jedis = this.pool.getResource()) {
            return jedis.get(key);
        }
    }

    public List<String> getMany(String... keys) {
        try (Jedis jedis = this.pool.getResource()) {
            return jedis.mget(keys);
        }
    }

    public List<String> getList(String key) {
        try (Jedis jedis = this.pool.getResource()) {
            return jedis.lrange(key, 0, -1);
        }
    }

    public long delete(String... keys) {
        Preconditions.checkArgument(keys.length > 0, "Keys cannot be empty");
        try (Jedis jedis = this.pool.getResource()) {
            return jedis.del(keys);
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = this.pool.getResource()) {
            return jedis.exists(key);
        }
    }
}
