package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    @FXML
    public AnchorPane factsPane;

    private final String APOD_KEY = "today:apod";

    private final int MAIN_APOD_WIDTH = 700;
    private final int MAIN_APOD_HEIGHT = 500;
    private final int SAVES_APOD_WIDTH = 900;
    private final int SAVES_APOD_HEIGHT = 700;

    private APOD theMainApod;

    private Gson gson;
    private RedisCacheService redisCacheService;
    private APODRepository apodRepository;

    public FactsApod(RedisCacheService redisCacheService, Gson gson, APODRepository apodRepository) {
        this.redisCacheService = redisCacheService;
        this.gson = gson;
        this.apodRepository = apodRepository;
    }

    @FXML
    public void initialize() {
        String todayApodJson = redisCacheService.get(APOD_KEY);

        saveBtn.setVisible(false);

        renderApod(todayApodJson);
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
                    theMainApod = vAPOD;

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
                    theMainApod = iAPOD;

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

            if(!apodRepository.existsByDate(theMainApod.getDate())){
                saveBtn.setVisible(true);
            }
        });
    }

    @FXML
    public void saveHandler(ActionEvent actionEvent) {
        if (!apodRepository.existsByDate(theMainApod.getDate())) {
            apodRepository.save(theMainApod);
            saveBtn.setText("Successfully Saved !");
            saveBtn.setDisable(true);
            new Thread(() -> {
                try {
                    Thread.sleep(15000);
                    saveBtn.setVisible(false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
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
            stage.setScene(scene);

            stage.setHeight(MAIN_APOD_HEIGHT);
            stage.setWidth(MAIN_APOD_WIDTH);

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

            factsLoader.setControllerFactory(param -> new SavesApod(redisCacheService, gson, apodRepository));
            AnchorPane factsAPOD = factsLoader.load();

            root.setCenter(factsAPOD);

            var scene = new Scene(root);
            stage.setScene(scene);

            stage.setHeight(SAVES_APOD_HEIGHT);
            stage.setWidth(SAVES_APOD_WIDTH);

            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
