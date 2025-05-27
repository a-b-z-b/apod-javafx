package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apod.layout.APODCard;
import org.apod.layout.StaggeredGridPane;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.apod.service.AbstractCacheService;
import org.apod.util.FXHelper;

import java.util.List;

public class SavesApod {
    private final String APOD_KEY = "list:apod";
    private final int APOD_TTL = 3600;

    private final int DETAILS_APOD_WIDTH = 850;
    private final int DETAILS_APOD_HEIGHT = 600;

    private Gson gson;
    private AbstractCacheService cacheService;
    private APODRepository apodRepository;

    @FXML
    public ScrollPane scrollablePane;
    @FXML
    public StackPane stackPane;
    @FXML
    public AnchorPane anchorPaneSaves;

    public SavesApod(AbstractCacheService cacheService, Gson gson, APODRepository apodRepository) {
        RuntimeTypeAdapterFactory<APOD> adapterFactory = RuntimeTypeAdapterFactory
                .of(APOD.class, "media_type")
                .registerSubtype(ImageAPOD.class, "image")
                .registerSubtype(VideoAPOD.class, "video");

        this.cacheService = cacheService;
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

                if (cacheService.get(APOD_KEY) != null) {
                    System.out.println("CACHE HIT");
                    String json = cacheService.get(APOD_KEY);
                    apods = gson.fromJson(json, new TypeToken<List<APOD>>() {}.getType());
                } else {
                    System.out.println("CACHE MISS");
                    apods = apodRepository.findAll();
                    String jsonAPODS = gson.toJson(apods);
                    cacheService.set(APOD_KEY, jsonAPODS, APOD_TTL);
                }

                return apods;
            }
        };

        loadSavesWithUI.setOnSucceeded(_ -> {
            List<APOD> apods = loadSavesWithUI.getValue();

            if (apods.isEmpty()) {
                Label infoLabel = new Label("There's no saves yet...");
                infoLabel.setId("infoLabel");
                infoLabel.setAlignment(Pos.CENTER);
                StackPane errorStackPane = new StackPane(infoLabel);// StackPane Centers content by default
                scrollablePane.setContent(errorStackPane);

                return;
            }

            StaggeredGridPane grid = new StaggeredGridPane(3, 50);

            // Reserve one empty card per APOD
            for (int i = 0; i < apods.size(); i++) {
                APODCard placeholder = new APODCard();
                grid.getChildren().add(placeholder);
            }

            // Populate grid elements with corresponding ImageView/WebView
            double colW = scrollablePane.getWidth() / 3 - grid.getGap();

            for (int i = 0; i < apods.size(); i++) {
                APOD apod = apods.get(i);
                APODCard card = (APODCard) grid.getChildren().get(i);
                card.setId(String.valueOf(apod.getId()));

                double h = 100 + (Math.random() * 100);
                card.setPrefSize(colW, h);

                if (apod instanceof ImageAPOD img) {
                    Image image = new Image(img.getHdurl(), true);
                    ImageView iv = new ImageView(image);
                    iv.setFitWidth(colW);
                    iv.setFitHeight(h);
                    iv.setPreserveRatio(true);

                    image.progressProperty().addListener((_, _, prog) -> {
                        if (prog.doubleValue() >= 1.0) {
                            Platform.runLater(() -> card.setContent(iv));
                        }
                    });
                    image.errorProperty().addListener((_, _, err) -> {
                        if (err) Platform.runLater(() -> {
                            card.setFallback("image");
                            card.setStyle("-fx-border-color: #f00");
                        });
                    });

                } else if (apod instanceof VideoAPOD vid) {
                    WebView wv = new WebView();
                    wv.setPrefHeight(h);

                    wv.getEngine().load(vid.getUrl());

                    wv.getEngine().getLoadWorker().stateProperty().addListener((_, _, s) -> {
                        if (s == Worker.State.FAILED) {
                            Platform.runLater(() -> {
                                card.setFallback("video");
                                card.setStyle("-fx-border-color: #f00");
                            });
                        }
                    });

                    Platform.runLater(() -> card.setContent(wv));
                }

                card.setOnMouseClicked(_ -> {
                    this.showDetailsOfTheAPOD(apod);
                });
            }

            Platform.runLater(() -> {
                scrollablePane.setContent(grid);
                grid.requestLayout();
            });
        });

        loadSavesWithUI.setOnFailed(_ -> {
            Label errorLabel = new Label("An Error Occurred...\nFailed to load saved APODs...\nTry Again Later.");
            errorLabel.setId("errorLabel");
            errorLabel.setAlignment(Pos.CENTER);
            StackPane errorStackPane = new StackPane(errorLabel);// StackPane Centers content by default
            scrollablePane.setContent(errorStackPane);
            loadSavesWithUI.getException().printStackTrace();
        });

        new Thread(loadSavesWithUI).start();
    }

    @FXML
    public void goHomeHandler(ActionEvent event) {
        FXHelper.switchToHome(event, cacheService, gson, apodRepository);
    }

    private void showDetailsOfTheAPOD(APOD apod) {
        try {
            FXMLLoader detailsLoader = new FXMLLoader();
            detailsLoader.setLocation(getClass().getResource("/fxml/details-apod.fxml"));

            detailsLoader.setControllerFactory(_ -> new DetailsApod(cacheService, gson, apodRepository, apod));

            AnchorPane detailsPane = detailsLoader.load();

            Scene scene = new Scene(detailsPane);
            scene.getStylesheets().add(getClass().getResource("/css/facts-apod.css").toExternalForm());

            Stage stage = (Stage) scrollablePane.getScene().getWindow();
            stage.setHeight(DETAILS_APOD_HEIGHT);
            stage.setWidth(DETAILS_APOD_WIDTH);
            stage.setScene(scene);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
