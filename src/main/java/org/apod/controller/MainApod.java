package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.apod.service.RedisCacheService;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainApod {
    private final String APOD_KEY = "today:apod";
    private final String LOADER_KEY = "loader:apod";
    private final int APOD_TTL = 3600;

    private final int FACTS_APOD_WIDTH = 850;
    private final int FACTS_APOD_HEIGHT = 600;
    private final int SAVES_APOD_WIDTH = 950;
    private final int SAVES_APOD_HEIGHT = 900;

    private Gson gson;
    private RedisCacheService redisCacheService;

    private APODRepository repository;

    private APOD theMainApod;

    public MainApod(RedisCacheService redisCacheService, Gson gson, APODRepository repository) {
        this.gson = gson;
        this.redisCacheService = redisCacheService;
        this.repository = repository;
    }

    @FXML
    public Label apodTitle;
    @FXML
    public Button fullscreenBtn;
    @FXML
    public ImageView todayApod;
    @FXML
    public WebView apodYtVideo;
    @FXML
    public Button factsBtn;
    @FXML
    public Button saveBtn;
    @FXML
    public Label saveActionToast;
    @FXML
    public MenuItem savesMenuItem;
    @FXML
    public WebView loader;

    @FXML
    public void initialize() {
        String todayApodJson = redisCacheService.get(APOD_KEY);

        String loaderMarkup = null;
        if(redisCacheService.get(LOADER_KEY) != null) {
            loaderMarkup = redisCacheService.get(LOADER_KEY);
        } else {
            loaderMarkup = this.loadHtmlToString("loader.html");
            redisCacheService.set(LOADER_KEY, loaderMarkup, APOD_TTL);
        }

        loader.getEngine().setUserStyleSheetLocation(getClass().getResource("/html/loader.css").toExternalForm());
        loader.getEngine().loadContent(loaderMarkup, "text/html");

        saveBtn.setVisible(false);
        factsBtn.setVisible(false);
        apodTitle.setVisible(false);
        fullscreenBtn.setVisible(false);

        if(todayApodJson != null) {
            renderApod(todayApodJson);
        } else {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest
                    .newBuilder(URI.create("https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY"))
                    .header("Accept", "application/json")
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(json -> {
                        // cache this json using redis
                        redisCacheService.set(APOD_KEY,json, APOD_TTL);
                        // render the APOD
                        renderApod(json);
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        }

        new Thread(() -> {
            try {
                Thread.sleep(9000);
                loader.setVisible(false);
                apodTitle.setVisible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void renderApod(String json) {

        Platform.runLater(() -> {
            APOD apod = gson.fromJson(json, new TypeToken<APOD>() {}.getType());
            theMainApod = apod;

            if (apod instanceof VideoAPOD) {
                apodTitle.setText(apod.getTitle());
                fullscreenBtn.setVisible(true);
                todayApod.setVisible(false);
                fullscreenBtn.setVisible(false);
                String ytEmbeddedVideo = ((VideoAPOD) apod).getUrl() + "&autoplay=1&mute=1&loop=1";
                apodYtVideo.getEngine().load(ytEmbeddedVideo);
                apodYtVideo.setVisible(true);
            } else if(apod instanceof ImageAPOD) {
                apodYtVideo.setVisible(false);
                todayApod.setImage(new Image(((ImageAPOD) apod).getHdurl()));
                todayApod.setVisible(true);
                fullscreenBtn.setVisible(true);
                apodTitle.setText(apod.getTitle());
            } else {
                apodTitle.setText("Unsupported media type.");
                todayApod.setVisible(false);
                apodYtVideo.setVisible(false);
            }

            if(!repository.existsByDate(theMainApod.getDate())){
                saveBtn.setVisible(true);
            }

            factsBtn.setVisible(true);
            if (!saveBtn.isVisible()) {
                factsBtn.setLayoutX(310);
                factsBtn.setLayoutY(542);
            }
        });
    }

    @FXML
    public void seeFactsHandler(ActionEvent event) {
        Node node = (Node) event.getSource();
        Stage stage = (Stage) node.getScene().getWindow();

        try {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(getClass().getResource("/fxml/root-apod.fxml"));

            FXMLLoader factsLoader = new FXMLLoader();
            factsLoader.setLocation(getClass().getResource("/fxml/facts-apod.fxml"));

            BorderPane root = rootLoader.load();

            factsLoader.setControllerFactory(param -> new FactsApod(redisCacheService, gson, repository));
            AnchorPane factsAPOD = factsLoader.load();

            root.setCenter(factsAPOD);

            var scene = new Scene(root);
            stage.setScene(scene);

            stage.setHeight(FACTS_APOD_HEIGHT);
            stage.setWidth(FACTS_APOD_WIDTH);

            scene.getStylesheets().add(getClass().getResource("/css/facts-apod.css").toExternalForm());

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void saveHandler(ActionEvent saveEvent) {
        if (!repository.existsByDate(theMainApod.getDate())) {
            repository.save(theMainApod);
            saveActionToast.setVisible(true);
            saveBtn.setVisible(false);
            new Thread(() -> {
                try {
                    Thread.sleep(15000);
                    saveActionToast.setVisible(false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    @FXML
    public void seeSavesHandler(ActionEvent event) {
        MenuItem menuItem = (MenuItem) event.getSource();

        Stage stage = (Stage) menuItem.getParentPopup().getOwnerWindow();

        try {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(getClass().getResource("/fxml/root-apod.fxml"));

            FXMLLoader factsLoader = new FXMLLoader();
            factsLoader.setLocation(getClass().getResource("/fxml/saves-apod.fxml"));

            BorderPane root = rootLoader.load();

            factsLoader.setControllerFactory(param -> new SavesApod(redisCacheService, gson, repository));
            AnchorPane factsAPOD = factsLoader.load();

            root.setCenter(factsAPOD);

            var scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/saves-apod.css").toExternalForm());

            stage.setScene(scene);

            stage.setHeight(SAVES_APOD_HEIGHT);
            stage.setWidth(SAVES_APOD_WIDTH);

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String loadHtmlToString(String path) {
        try {
            URL markupPath = getClass().getResource("/html/" + path);
            if (markupPath == null) {
                throw new RuntimeException("Could not find html file: " + path);
            }
            return Files.readString(Paths.get(markupPath.toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
