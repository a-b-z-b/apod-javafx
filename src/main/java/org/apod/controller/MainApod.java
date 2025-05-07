package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
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

    public MainApod(RedisCacheService redisCacheService, Gson gson) {
        this.gson = gson;
        this.redisCacheService = redisCacheService;
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
    public void initialize() {
        String todayApodJson = redisCacheService.get(APOD_KEY);

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
}
