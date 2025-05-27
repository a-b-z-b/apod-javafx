package org.apod.e2e;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
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
import org.junit.Assert;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import static org.testfx.matcher.control.LabeledMatchers.hasText;

public class SaveMainAPODTest extends ApplicationTest {
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

        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-up.sql");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-apod-test.fxml"));
        loader.setControllerFactory(_ -> new MainApod(redis, gson, repository));
        AnchorPane pane = loader.load();

        Scene scene = new Scene(pane);
        scene.getStylesheets().add(APODApp.class.getResource("/css/main-apod.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        stage.toFront();
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

    @Test
    public void save_main_apod_flow() throws Exception {
        FxRobot bot = new FxRobot();

        FxAssert.verifyThat("#saveBtn", NodeMatchers.isNotNull());

        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, () -> {
            WebView loader = bot.lookup("#loader").query();
            return !loader.isVisible();
        });

        bot.clickOn("#saveBtn");
        FxAssert.verifyThat("#saveBtn", NodeMatchers.isInvisible());

        FxAssert.verifyThat("#saveActionToast", NodeMatchers.isNotNull());
        FxAssert.verifyThat("#saveActionToast", NodeMatchers.isVisible());
        FxAssert.verifyThat("#saveActionToast", hasText("Successfully Saved !"));

        FxAssert.verifyThat("#factsBtn", NodeMatchers.isVisible());
        Button factsBtn = bot.lookup("#factsBtn").queryButton();
        Assert.assertTrue(factsBtn.getLayoutX() == 310 && factsBtn.getLayoutY() == 542);
    }

    /*@Test
    public void no_internet_simulation() throws Exception {
        // TODO: re-implement this test case after abstracting the APOD fetch to some service class...
        FxRobot bot = new FxRobot();

        Thread.sleep(21000);

        FxAssert.verifyThat("#apodTitle", hasText("Error loading media...\nCheck your internet connection."));

        ImageView fallBackImg = bot.lookup("#todayApod").query();
        Assert.assertEquals(APODApp.class.getResource("/assets/broken-1.jpg").toExternalForm(), fallBackImg.getImage().getUrl());
    }*/

    @After
    public void tearDown() throws Exception {
        MigrationsRunner.runTestMigrations(connection, "db/migrations/test-down.sql");
    }
}
