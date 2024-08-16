package de.redstonecloud.api.encryption.cache;

import de.redstonecloud.api.redis.cache.Cache;
import de.redstonecloud.api.util.B64;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class KeyCache extends Cache {

    public void addKey(String route, PublicKey key) {
        List<String> routeList = this.getList(route);
        if (routeList == null) {
            routeList = new ArrayList<>();
        }

        byte[] keyBytes = key.getEncoded();
        routeList.add(B64.encode(keyBytes));
        this.setList("encryption:" + route, routeList);
    }

    public void removeKey(String route, PublicKey key) {
        List<String> routeList = this.getList("encryption." + route);
        if (routeList == null) {
            return;
        }

        byte[] keyBytes = key.getEncoded();
        if (routeList.removeIf(k -> k.equals(B64.encode(keyBytes)))) {
            this.setList("encryption:" + route, routeList);
        }
    }
}
