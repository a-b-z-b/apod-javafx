package org.apod.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apod.controller.MainApod;
import org.apod.data.DBConnection;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.apod.service.RedisCacheService;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;

public class LoadMainSceneTest extends ApplicationTest {
    private RedisCacheService redis;
    private Gson gson;
    private APODRepository repository;

    @FXML
    @Override
    public void start(Stage stage) throws Exception {
        this.redis = new RedisCacheService("localhost", 6379);
        this.gson = new GsonBuilder().registerTypeAdapterFactory(RuntimeTypeAdapterFactory
                .of(APOD.class, "media_type")
                .registerSubtype(ImageAPOD.class, "image")
                .registerSubtype(VideoAPOD.class, "video")
        ).create();
        this.repository = new APODRepository(DBConnection.getConnection("test"));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-apod-test.fxml"));
        loader.setControllerFactory(_ -> new MainApod(redis, gson, repository));
        AnchorPane pane = loader.load();

        Scene scene = new Scene(pane);
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
    }

    @Test
    public void has_menuBtn_button() throws Exception {
        FxAssert.verifyThat("#menuBtn", NodeMatchers.isNotNull());
    }

    @Test
    public void has_loader() throws Exception {
        FxAssert.verifyThat("#loader", NodeMatchers.isNotNull());
    }
}
