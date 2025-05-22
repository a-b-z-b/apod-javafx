package org.apod.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import org.sqlite.SQLiteException;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;

public class SaveAPODTest {
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

        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-up.sql");
    }

    @Test
    public void create_new_apod_test() throws Exception {
        String json = this.readJsonFromFile(getClass().getResource("/data/sample-today-apod.json"));
        APOD apod = gson.fromJson(json, new TypeToken<APOD>(){}.getType());

        repository.save(apod);

        APOD fetchedApod = repository.findById(1);

        Assert.assertNotNull(apod);
        Assert.assertNotNull(fetchedApod);
        Assert.assertEquals(apod.getTitle(), fetchedApod.getTitle());
        Assert.assertEquals(apod.getDate(), fetchedApod.getDate());
        Assert.assertEquals(apod.getExplanation(), fetchedApod.getExplanation());
        Assert.assertEquals(apod.getMedia_type(), fetchedApod.getMedia_type());

        if (apod instanceof ImageAPOD && fetchedApod instanceof ImageAPOD) {
            Assert.assertEquals(((ImageAPOD) apod).getHdurl(), ((ImageAPOD) fetchedApod).getHdurl());
        } else if (apod instanceof VideoAPOD && fetchedApod instanceof VideoAPOD) {
            Assert.assertEquals(((VideoAPOD) apod).getUrl(), ((VideoAPOD) fetchedApod).getUrl());
        }

        RuntimeException rtex = Assert.assertThrows(RuntimeException.class, () -> repository.save(apod));
        Assert.assertTrue(rtex.getCause() instanceof SQLiteException);
    }

    @After
    public void tearDown() throws Exception {
        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-down.sql");
        if (connection != null) {
            connection.close();
        }
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
