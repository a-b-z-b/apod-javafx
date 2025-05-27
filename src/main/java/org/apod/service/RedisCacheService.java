package org.apod.service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisCacheService implements AbstractCacheService {
    private JedisPool jedisPool;

    public RedisCacheService(String host, int port) {
        this.jedisPool = new JedisPool(host, port);
    }

    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    public void set(String key, String value, int ttlSeconds) {
        try(Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, ttlSeconds, value);
        }
    }

    public void clearCache() {
        try(Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    public void shutDown(){
        jedisPool.close();
    }

    public static boolean isRedisAvailable(String host, int port) {
        try (Jedis jedis = new Jedis(host, port)) {
            return jedis.ping().equals("PONG");
        } catch (Exception e) {
            System.err.println("Redis unavailable: " + e.getMessage());
            return false;
        }
    }
}
