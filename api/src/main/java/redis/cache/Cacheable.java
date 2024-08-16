package redis.cache;

public abstract class Cacheable {
    public void updateCache() {
        new Cache().set(cacheKey(), this.toString());
    }

    public void resetCache() {
        new Cache().delete(cacheKey());
    }

    public abstract String cacheKey();
}
