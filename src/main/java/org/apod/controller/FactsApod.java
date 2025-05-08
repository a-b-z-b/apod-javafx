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

import java.text.SimpleDateFormat;

public class FactsApod {
    @FXML
    public ImageView apodImage;
    @FXML
    public WebView apodYtVideo;
    @FXML
    public Label titleAPOD;
    @FXML
    public Label explanationAPOD;
    @FXML
    public Label cpRightPhotographer;
    @FXML
    public Label dateAPOD;
    @FXML
    public Button saveBtn;

    private final String APOD_KEY = "today:apod";

    private Gson gson;
    private RedisCacheService redisCacheService;

    public FactsApod(RedisCacheService redisCacheService, Gson gson) {
        this.redisCacheService = redisCacheService;
        this.gson = gson;
    }

    @FXML
    public void initialize() {
        String todayApodJson = redisCacheService.get(APOD_KEY);

        saveBtn.setDisable(true);

        renderApod(todayApodJson);

        saveBtn.setDisable(false);
    }

    public void renderApod(String json) {

        if(json == null) {
            return;
        }

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String mediaType = obj.get("media_type").getAsString();

        Platform.runLater(() -> {

            switch (mediaType) {
                case "video":
                    VideoAPOD vAPOD = gson.fromJson(json, VideoAPOD.class);

                    apodImage.setVisible(false);

                    titleAPOD.setText(vAPOD.getTitle());
                    String ytEmbeddedVideo = vAPOD.getUrl() + "&autoplay=1&mute=1&loop=1";

                    apodYtVideo.getEngine().load(ytEmbeddedVideo);
                    apodYtVideo.setVisible(true);

                    cpRightPhotographer.setText("APOD NASA API.");
                    explanationAPOD.setText(vAPOD.getExplanation());
                    dateAPOD.setText(new SimpleDateFormat("yyyy-MM-dd").format(vAPOD.getDate()));

                    break;
                case "image":
                    ImageAPOD iAPOD = gson.fromJson(json, ImageAPOD.class);

                    apodYtVideo.setVisible(false);

                    if (iAPOD.getCopyright() != null) {
                        cpRightPhotographer.setText(iAPOD.getCopyright().trim());
                    } else {
                        cpRightPhotographer.setText("APOD NASA API.");
                    }

                    apodImage.setImage(new Image(iAPOD.getHdurl()));
                    apodImage.setVisible(true);

                    titleAPOD.setText(iAPOD.getTitle());
                    explanationAPOD.setText(iAPOD.getExplanation().trim());
                    dateAPOD.setText(new SimpleDateFormat("yyyy-MM-dd").format(iAPOD.getDate()));

                    break;
                default:
                    titleAPOD.setText("Unsupported media type.");
                    apodImage.setVisible(false);
                    apodYtVideo.setVisible(false);
                    break;
            }
        });
    }

}
