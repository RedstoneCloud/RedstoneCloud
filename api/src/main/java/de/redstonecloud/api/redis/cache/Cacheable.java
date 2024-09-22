package de.redstonecloud.api.redis.cache;

public interface Cacheable {
    default void updateCache() {
        new Cache().set(cacheKey(), this.toString());
    }

    default void resetCache() {
        new Cache().delete(cacheKey());
    }

    String cacheKey();
}
