package org.apod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import org.apod.controller.MainApod;
import org.apod.data.DBConnection;
import org.apod.data.MigrationsRunner;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.apod.service.RedisCacheService;

import java.sql.Connection;

/**
 * JavaFX App
 */
public class APODApp extends Application {
    private final int MAIN_APOD_WIDTH = 700;
    private final int MAIN_APOD_HEIGHT = 700;

    private Stage stage;
    private BorderPane root;

    private RedisCacheService cacheService;
    private Gson gson;
    private RuntimeTypeAdapterFactory<APOD> adapterFactory;
    private APODRepository apodRepository;

    private Connection connection;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.stage.setTitle("Astronomy Picture Of the Day");

        this.cacheService = new RedisCacheService("localhost", 6379);
        this.adapterFactory = RuntimeTypeAdapterFactory
                .of(APOD.class, "media_type")
                .registerSubtype(ImageAPOD.class, "image")
                .registerSubtype(VideoAPOD.class, "video");
        this.gson = new GsonBuilder()
                .registerTypeAdapterFactory(adapterFactory)
                .create();

        this.connection = DBConnection.getConnection("sqlite");

        this.apodRepository = new APODRepository(this.connection);

        MigrationsRunner.runMigrations(connection, "/db/migrations/sqlite/init.sql");
//        MigrationsRunner.runMigrations(connection, "/db/migrations/sqlite/seed.sql");

        initRootLayout();

        showMainAPOD();
    }

    @Override
    public void stop() throws Exception {
        // cleanly release resources
        if (cacheService != null) {
            cacheService.shutDown();
        }

        if (gson != null) {
            gson = null;
        }
    }

    public void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/root-apod.fxml"));

            root = loader.load();
            var scene = new Scene(root);
            stage.setScene(scene);

            scene.getStylesheets().add(getClass().getResource("/css/main-apod.css").toExternalForm());

            stage.setHeight(MAIN_APOD_HEIGHT);
            stage.setWidth(MAIN_APOD_WIDTH);

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void showMainAPOD() {
        try {

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/main-apod.fxml"));

            // Inject Dependencies of the Controller
            loader.setControllerFactory(param -> new MainApod(cacheService, gson, apodRepository));

            AnchorPane mainAPOD = loader.load();
            root.setCenter(mainAPOD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch();
    }

}