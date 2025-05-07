package org.apod;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import org.apod.controller.MainApod;
import org.apod.service.RedisCacheService;

/**
 * JavaFX App
 */
public class APODApp extends Application {
    private Stage stage;
    private BorderPane root;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.stage.setTitle("Astronomy Picture Of the Day");

        initRootLayout();

        showMainAPOD();
    }

    public void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/root-apod.fxml"));

            root = loader.load();
            var scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void showMainAPOD() {
        try {
            RedisCacheService cacheService = new RedisCacheService("localhost", 6379);
            Gson gson = new Gson();

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/main-apod.fxml"));

            // Inject Dependencies of the Controller
            loader.setControllerFactory(param -> new MainApod(cacheService, gson));

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