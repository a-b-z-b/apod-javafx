package org.apod.service;

import java.util.concurrent.ConcurrentHashMap;

public class DefaultCacheService implements AbstractCacheService {
    private ConcurrentHashMap<String,String> cache;

    public DefaultCacheService() {
        cache = new ConcurrentHashMap<>();
    }

    public String get(String key) {
        return cache.get(key);
    }

    public void set(String key, String value, int ttl) {
        cache.put(key, value);
    }

    public void clearCache() {
        cache.clear();
    }
}
