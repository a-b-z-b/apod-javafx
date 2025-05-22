package org.apod.integration;

import org.apod.data.DBConnection;
import org.apod.data.MigrationsRunner;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.repository.APODRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;

public class GetAPODByIDTest {
    private APODRepository repository;
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        connection = DBConnection.getConnection("test");
        repository = new APODRepository(connection);

        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-up.sql", "db/migrations/test-seed.sql");
    }

    @Test
    public void get_apod_by_id() throws Exception {
        APOD apod = repository.findById(3);

        Assert.assertTrue(apod instanceof ImageAPOD);
        Assert.assertEquals("image", apod.getMedia_type());
        Assert.assertNotNull(((ImageAPOD) apod).getHdurl());
    }

    @After
    public void tearDown() throws Exception {
        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-down.sql");
        if (connection != null) {
            connection.close();
        }
    }
}
