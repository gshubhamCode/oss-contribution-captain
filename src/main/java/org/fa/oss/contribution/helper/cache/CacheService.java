package org.fa.oss.contribution.helper.cache;

public interface CacheService<T> {
    T load();
    void save(T data);

    boolean isCacheValid();
     long getCacheAgeMillis();
}

