package org.apod.unit;

import org.apod.service.RedisCacheService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class CachingTest {
    private final int APOD_TTL_TEST_SECONDS = 5;
    private final String APOD_KEY_TEST = "apod:today";

    private RedisCacheService redis;

    @Before
    public void setUp() {
        this.redis = new RedisCacheService("localhost", 6379);
    }

    @Test
    public void has_same_data() {
        String json = this.readJsonFromFile(getClass().getResource("/data/sample-today-apod.json"));
        this.redis.set(APOD_KEY_TEST, json, APOD_TTL_TEST_SECONDS);

        String valueFromRedis = redis.get(APOD_KEY_TEST);
        assertNotNull("Redis should return a value immediately after set", valueFromRedis);
        assertEquals("Stored value should match original JSON", json, this.redis.get(APOD_KEY_TEST));

        try {
            Thread.sleep((APOD_TTL_TEST_SECONDS + 1) * 1000L);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }

        assertNull("Redis value should be null after expiration", redis.get(APOD_KEY_TEST));
    }

    @After
    public void tearDown() {
        this.redis.shutDown();
    }

    private String readJsonFromFile(URL path) {
        try {
            return Files.readString(Paths.get(path.toURI()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
