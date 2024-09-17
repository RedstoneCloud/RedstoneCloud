package de.redstonecloud.api.encryption.cache;

import de.redstonecloud.api.redis.cache.Cache;
import de.redstonecloud.api.util.B64;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import lombok.Getter;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KeyCache extends Cache {

    public static class Holder {
        @Getter private static KeyCache cache;
    }

    public KeyCache() {
        super();

        Holder.cache = this;
    }

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

    public Collection<PublicKey> getKeys(String route) {
        List<String> stringKeys = this.getList(route);

        ObjectArraySet<PublicKey> keys = new ObjectArraySet<>();
        for (String stringKey : stringKeys) {
            try {
                byte[] keyBytes = B64.decode(stringKey);
                KeyFactory factory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                PublicKey key = factory.generatePublic(spec);

                keys.add(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return keys;
    }
}
