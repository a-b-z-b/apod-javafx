package org.apod.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.apod.data.DBConnection;
import org.apod.data.MigrationsRunner;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.util.List;

public class GetAllAPODsTest {
    private APODRepository repository;
    private Connection connection;
    private Gson gson;

    @Before
    public void setUp() throws Exception {
        connection = DBConnection.getConnection("test");
        repository = new APODRepository(connection);
        gson = new GsonBuilder().registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory.of(APOD.class, "media_type")
                        .registerSubtype(ImageAPOD.class, "image")
                        .registerSubtype(VideoAPOD.class, "video")
        ).create();

        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-up.sql", "db/migrations/test-seed.sql");
    }

    @Test
    public void get_all_stored_apods() throws Exception {
        List<APOD> apods = repository.findAll();
        Assert.assertNotNull(apods);

        Assert.assertFalse(apods.isEmpty());
        Assert.assertNotNull(apods.getFirst());

        Assert.assertNotNull(apods.getLast());

        Assert.assertEquals(5, apods.size());
    }

    @After
    public void tearDown() throws Exception {
        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-down.sql");
        if (connection != null) {
            connection.close();
        }
    }
}
