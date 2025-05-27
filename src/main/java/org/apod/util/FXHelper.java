package org.apod.util;

import com.google.gson.Gson;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apod.controller.FactsApod;
import org.apod.controller.MainApod;
import org.apod.controller.SavesApod;
import org.apod.repository.APODRepository;
import org.apod.service.AbstractCacheService;

public class FXHelper {
    private static final int MAIN_APOD_WIDTH = 700;
    private static final int MAIN_APOD_HEIGHT = 700;

    private static final int SAVES_APOD_WIDTH = 950;
    private static final int SAVES_APOD_HEIGHT = 900;

    private static final int FACTS_APOD_WIDTH = 850;
    private static final int FACTS_APOD_HEIGHT = 600;

    public static void switchToHome(ActionEvent event, AbstractCacheService cacheService, Gson gson, APODRepository apodRepository) {
        MenuItem menuItem = (MenuItem) event.getSource();

        Stage stage = (Stage) menuItem.getParentPopup().getOwnerWindow();

        try {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(FXHelper.class.getResource("/fxml/root-apod.fxml"));

            FXMLLoader mainLoader = new FXMLLoader();
            mainLoader.setLocation(FXHelper.class.getResource("/fxml/main-apod.fxml"));

            BorderPane root = rootLoader.load();

            mainLoader.setControllerFactory(_ -> new MainApod(cacheService, gson, apodRepository));
            AnchorPane homeAPOD = mainLoader.load();

            root.setCenter(homeAPOD);

            var scene = new Scene(root);
            scene.getStylesheets().add(FXHelper.class.getResource("/css/main-apod.css").toExternalForm());

            stage.setScene(scene);

            stage.setHeight(MAIN_APOD_HEIGHT);
            stage.setWidth(MAIN_APOD_WIDTH);

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void switchToSaves(ActionEvent event, AbstractCacheService cacheService, Gson gson, APODRepository apodRepository) {
        MenuItem menuItem = (MenuItem) event.getSource();

        Stage stage = (Stage) menuItem.getParentPopup().getOwnerWindow();

        try {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(FXHelper.class.getResource("/fxml/root-apod.fxml"));

            FXMLLoader savesLoader = new FXMLLoader();
            savesLoader.setLocation(FXHelper.class.getResource("/fxml/saves-apod.fxml"));

            BorderPane root = rootLoader.load();

            savesLoader.setControllerFactory(_ -> new SavesApod(cacheService, gson, apodRepository));
            AnchorPane savesAPOD = savesLoader.load();

            root.setCenter(savesAPOD);

            var scene = new Scene(root);
            scene.getStylesheets().add(FXHelper.class.getResource("/css/saves-apod.css").toExternalForm());

            stage.setScene(scene);

            stage.setHeight(SAVES_APOD_HEIGHT);
            stage.setWidth(SAVES_APOD_WIDTH);

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void switchToFacts(ActionEvent event, AbstractCacheService cacheService, Gson gson, APODRepository apodRepository) {
        Node node = (Node) event.getSource();
        Stage stage = (Stage) node.getScene().getWindow();

        try {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(FXHelper.class.getResource("/fxml/root-apod.fxml"));

            FXMLLoader factsLoader = new FXMLLoader();
            factsLoader.setLocation(FXHelper.class.getResource("/fxml/facts-apod.fxml"));

            BorderPane root = rootLoader.load();

            factsLoader.setControllerFactory(_ -> new FactsApod(cacheService, gson, apodRepository));
            AnchorPane factsAPOD = factsLoader.load();

            root.setCenter(factsAPOD);

            var scene = new Scene(root);
            stage.setScene(scene);

            stage.setHeight(FACTS_APOD_HEIGHT);
            stage.setWidth(FACTS_APOD_WIDTH);

            scene.getStylesheets().add(FXHelper.class.getResource("/css/facts-apod.css").toExternalForm());

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
