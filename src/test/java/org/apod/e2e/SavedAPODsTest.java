package org.apod.e2e;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apod.APODApp;
import org.apod.controller.MainApod;
import org.apod.data.DBConnection;
import org.apod.data.MigrationsRunner;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.apod.service.RedisCacheService;
import org.junit.After;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.base.ParentMatchers;

import java.sql.Connection;

public class SavedAPODsTest extends ApplicationTest {
    private RedisCacheService redis;
    private Gson gson;
    private APODRepository repository;
    private Connection connection;

    @FXML
    @Override
    public void start(Stage stage) throws Exception {
        this.redis = new RedisCacheService("localhost", 6379);
        // simulate fresh fetch from API endpoint
        this.redis.clearCache();
        this.gson = new GsonBuilder().registerTypeAdapterFactory(RuntimeTypeAdapterFactory
                .of(APOD.class, "media_type")
                .registerSubtype(ImageAPOD.class, "image")
                .registerSubtype(VideoAPOD.class, "video")
        ).create();
        this.connection = DBConnection.getConnection("test");
        this.repository = new APODRepository(this.connection);

        // we need this database migration setup because we're doing the check by date whether the APOD is already saved in db.
        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-up.sql", "db/migrations/test-seed.sql");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-apod-test.fxml"));
        loader.setControllerFactory(_ -> new MainApod(redis, gson, repository));
        AnchorPane pane = loader.load();

        Scene scene = new Scene(pane);
        scene.getStylesheets().add(APODApp.class.getResource("/css/main-apod.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        stage.toFront();
    }

    @Test
    public void see_saves_flow() throws Exception {
        FxRobot bot = new FxRobot();

        FxAssert.verifyThat("#menuBtn", NodeMatchers.isNotNull());

        bot.clickOn("#menuBtn");
        bot.clickOn("#savesMenuItem");

        FxAssert.verifyThat("#grid", NodeMatchers.isVisible());
        FxAssert.verifyThat("#grid", ParentMatchers.hasChildren(5));
    }

    @FXML
    @Override
    public void stop() throws Exception {
        if (redis != null) {
            redis.shutDown();
        }

        if (gson != null) {
            gson = null;
        }
        if (connection != null) {
            this.connection.close();
        }
    }

    @After
    public void tearDown() throws Exception {
        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-down.sql");
    }
}
