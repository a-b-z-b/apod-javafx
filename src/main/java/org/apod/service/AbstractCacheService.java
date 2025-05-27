package org.apod.service;

public interface AbstractCacheService {
    public String get(String key);
    public void set(String key, String value, int ttl);
}
