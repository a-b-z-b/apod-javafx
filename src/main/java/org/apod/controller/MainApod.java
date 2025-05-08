package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.apod.repository.Repository;
import org.apod.service.RedisCacheService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MainApod {
    private final String APOD_KEY = "today:apod";
    private final int APOD_TTL = 3600;

    private Gson gson;
    private RedisCacheService redisCacheService;

    private APODRepository repository;

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
    public void initialize() {
        String todayApodJson = redisCacheService.get(APOD_KEY);

        saveBtn.setDisable(true);
        factsBtn.setDisable(true);

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

        saveBtn.setDisable(false);
        factsBtn.setDisable(false);
    }

    public void renderApod(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String mediaType = obj.get("media_type").getAsString();

        Platform.runLater(() -> {

            switch (mediaType) {
                case "video":
                    VideoAPOD vAPOD = gson.fromJson(json, VideoAPOD.class);
                    apodTitle.setText(vAPOD.getTitle());
                    fullscreenBtn.setVisible(true);
                    todayApod.setVisible(false);
                    fullscreenBtn.setVisible(false);
                    String ytEmbeddedVideo = vAPOD.getUrl() + "&autoplay=1&mute=1&loop=1";
                    apodYtVideo.getEngine().load(ytEmbeddedVideo);
                    apodYtVideo.setVisible(true);
                    break;
                case "image":
                    ImageAPOD iAPOD = gson.fromJson(json, ImageAPOD.class);
                    apodYtVideo.setVisible(false);
                    todayApod.setImage(new Image(iAPOD.getHdurl()));
                    todayApod.setVisible(true);
                    apodTitle.setText(iAPOD.getTitle());
                    break;
                default:
                    apodTitle.setText("Unsupported media type.");
                    todayApod.setVisible(false);
                    apodYtVideo.setVisible(false);
                    break;
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

            factsLoader.setControllerFactory(param -> new FactsApod(redisCacheService, gson));
            AnchorPane factsAPOD = factsLoader.load();

            root.setCenter(factsAPOD);

            var scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void saveHandler(ActionEvent saveEvent) {
        // save the apod into sqlite
        // 1- depending on media-type u choose repository class.
        // 2- use the repository methods to store the apod.
        // 3- notify user that operation succeeded via a toast like component.
    }
}
