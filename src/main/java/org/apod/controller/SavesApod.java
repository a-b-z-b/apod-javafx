package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
                .create();;
        this.apodRepository = apodRepository;
    }

    @FXML
    public void initialize() {
        StaggeredGridPane grid = new StaggeredGridPane(3, 100);

        scrollablePane.setFitToWidth(true);
        scrollablePane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollablePane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        List<APOD> apods = null;

        if (redisCacheService.get(APOD_KEY) != null) {
            System.out.println("CACHE HIT");
            String json = redisCacheService.get(APOD_KEY);
            apods = gson.fromJson(json, new TypeToken<List<APOD>>() {}.getType());
        } else {
            System.out.println("CACHE MISS");
            apods = apodRepository.findAll();
            String jsonAPODS = this.gson.toJson(apods);
            redisCacheService.set(APOD_KEY, jsonAPODS, APOD_TTL);
        }

        for (int i = 0; i < apods.size(); i++) {
            double height = 100 + (Math.random() * 100);

            if (apods.get(i) instanceof VideoAPOD) {
                WebView wv = new WebView();
                wv.getEngine().load(((VideoAPOD) apods.get(i)).getUrl());

                wv.setPrefHeight(height);
//                wv.setStyle("-fx-border-radius: 25px;");
                grid.getChildren().add(wv);
            } else if (apods.get(i) instanceof ImageAPOD) {
                ImageView iv = new ImageView();

                iv.setImage(new Image(((ImageAPOD) apods.get(i)).getHdurl()));

                iv.setFitHeight(height);
                iv.setPreserveRatio(true);
//                iv.setStyle("-fx-border-radius: 25px;");
                grid.getChildren().add(iv);
            }
        }

        // Size binding to make it responsive
        grid.prefWidthProperty().bind(scrollablePane.widthProperty());
        grid.prefHeightProperty().bind(scrollablePane.heightProperty());

        scrollablePane.setContent(grid);
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
