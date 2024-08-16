package encryption.cache;

import redis.cache.Cache;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import util.B64;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class KeyCache extends Cache {

    @Override
    protected JedisPool createJedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(2);
        config.setMaxIdle(4);
        config.setMaxTotal(8);
        config.setBlockWhenExhausted(true);
        return new JedisPool(config);
    }

    public void addKey(String route, PublicKey key) {
        List<String> routeList = this.getList(route);
        if (routeList == null) {
            routeList = new ArrayList<>();
        }

        byte[] keyBytes = key.getEncoded();
        routeList.add(B64.encode(keyBytes));
        this.setList("encryption." + route, routeList);
    }

    public void removeKey(String route, PublicKey key) {
        List<String> routeList = this.getList("encryption." + route);
        if (routeList == null) {
            return;
        }

        byte[] keyBytes = key.getEncoded();
        if (routeList.removeIf(k -> k.equals(B64.encode(keyBytes)))) {
            this.setList("encryption." + route, routeList);
        }
    }
}
