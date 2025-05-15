package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apod.layout.StaggeredGridPane;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.apod.service.RedisCacheService;

import java.util.List;

public class SavesApod {
    private final String LOADER_KEY = "loader:apod";
    private final String APOD_KEY = "list:apod";
    private final int APOD_TTL = 3600;

    private final int MAIN_APOD_WIDTH = 700;
    private final int MAIN_APOD_HEIGHT = 700;

    private Gson gson;
    private RedisCacheService redisCacheService;
    private APODRepository apodRepository;

    @FXML
    public ScrollPane scrollablePane;

    public SavesApod(RedisCacheService redisCacheService, Gson gson, APODRepository apodRepository) {
        RuntimeTypeAdapterFactory<APOD> adapterFactory = RuntimeTypeAdapterFactory
                .of(APOD.class, "media_type")
                .registerSubtype(ImageAPOD.class, "image")
                .registerSubtype(VideoAPOD.class, "video");

        this.redisCacheService = redisCacheService;
        this.gson = new GsonBuilder()
                .registerTypeAdapterFactory(adapterFactory)
                .create();
        this.apodRepository = apodRepository;
    }

    @FXML
    public void initialize() {
        Task<List<APOD>> loadSavesWithUI = new Task<>() {
            @Override
            protected List<APOD> call() {
                List<APOD> apods = null;

                if (redisCacheService.get(APOD_KEY) != null) {
                    System.out.println("CACHE HIT");
                    String json = redisCacheService.get(APOD_KEY);
                    apods = gson.fromJson(json, new TypeToken<List<APOD>>() {}.getType());
                } else {
                    System.out.println("CACHE MISS");
                    apods = apodRepository.findAll();
                    String jsonAPODS = gson.toJson(apods);
                    redisCacheService.set(APOD_KEY, jsonAPODS, APOD_TTL);
                }

                return apods;
            }
        };

        loadSavesWithUI.setOnSucceeded(event -> {
            List<APOD> apods = loadSavesWithUI.getValue();

            StaggeredGridPane grid = new StaggeredGridPane(3, 100);
            grid.setId("staggered-grid");

            for (APOD apod : apods) {
                double height = 100 + (Math.random() * 100);

                if (apod instanceof VideoAPOD) {
                    Platform.runLater(() -> {
                        WebView wv = new WebView();
                        wv.getEngine().load(((VideoAPOD) apod).getUrl());

                        wv.setPrefHeight(height);
                        grid.getChildren().add(wv);
                    });
                } else if (apod instanceof ImageAPOD) {
                    ImageView iv = new ImageView();

                    iv.setImage(new Image(((ImageAPOD) apod).getHdurl(), true)); // HERE THE SECRET IS THE BOOLEAN! "async load"

                    iv.setFitHeight(height);
                    iv.setPreserveRatio(true);
                    grid.getChildren().add(iv);
                }
            }
            scrollablePane.setContent(grid);
        });
        loadSavesWithUI.setOnFailed(event -> {
            scrollablePane.setContent(new Label("Failed to load APODs."));
            loadSavesWithUI.getException().printStackTrace();
        });

        new Thread(loadSavesWithUI).start();
    }

    @FXML
    public void goHomeHandler(ActionEvent event) {
        MenuItem menuItem = (MenuItem) event.getSource();

        Stage stage = (Stage) menuItem.getParentPopup().getOwnerWindow();

        try {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(getClass().getResource("/fxml/root-apod.fxml"));

            FXMLLoader factsLoader = new FXMLLoader();
            factsLoader.setLocation(getClass().getResource("/fxml/main-apod.fxml"));

            BorderPane root = rootLoader.load();

            factsLoader.setControllerFactory(param -> new MainApod(redisCacheService, gson, apodRepository));
            AnchorPane factsAPOD = factsLoader.load();

            root.setCenter(factsAPOD);

            var scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/main-apod.css").toExternalForm());

            stage.setScene(scene);

            stage.setHeight(MAIN_APOD_HEIGHT);
            stage.setWidth(MAIN_APOD_WIDTH);

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
