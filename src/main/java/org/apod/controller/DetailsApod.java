package org.apod.controller;

import com.google.gson.Gson;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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

public class DetailsApod {
    private final String LOADER_KEY = "loader:apod";
    private final String FAIL_TEXT = "FAILED TO LOAD :/";

    private final int MAIN_APOD_WIDTH = 700;
    private final int MAIN_APOD_HEIGHT = 700;
    private final int SAVES_APOD_WIDTH = 950;
    private final int SAVES_APOD_HEIGHT = 900;

    private Gson gson;
    private RedisCacheService redisCacheService;
    private APODRepository repository;

    private APOD apod;

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
    public AnchorPane detailsPane;
    @FXML
    public WebView loader;

    public DetailsApod(RedisCacheService redisCacheService, Gson gson, APODRepository repository, APOD apodData) {
        this.redisCacheService = redisCacheService;
        this.gson = gson;
        this.repository = repository;
        this.apod = apodData;
    }

    @FXML
    public void initialize() {
        String loaderMarkup = redisCacheService.get(LOADER_KEY);
        loader.getEngine().setUserStyleSheetLocation(getClass().getResource("/html/loader.css").toExternalForm());
        loader.getEngine().loadContent(loaderMarkup, "text/html");

        if (apod == null) {
            throw new RuntimeException("APOD cannot be null at this point.");
        }

        titleAPOD.setText(apod.getTitle());
        dateAPOD.setText(new SimpleDateFormat("yyyy-MM-dd").format(apod.getDate()));
        explanationAPOD.setText(apod.getExplanation());

        if (apod instanceof ImageAPOD img) {
            apodYtVideo.setVisible(false);

            Image image = new Image(img.getHdurl(), true);

            image.errorProperty().addListener((_, _, isNowError) -> {
                if(isNowError) {
                    Image fallBackImage = new Image(getClass().getResource("/assets/broken-3.jpg").toExternalForm(), true);
                    apodImage.setImage(fallBackImage);
                    apodImage.setVisible(true);

                    loader.setVisible(false);
                    apodYtVideo.setVisible(false);

                    explanationAPOD.setText(FAIL_TEXT);
                    dateAPOD.setText(FAIL_TEXT);
                    cpRightPhotographer.setText(FAIL_TEXT);

                    titleAPOD.setText("Error loading media...");
                    titleAPOD.setStyle("-fx-text-fill: red; -fx-font-size: 15px");
                }
            });

            image.progressProperty().addListener((_, _, newValue) -> {
                if (newValue.doubleValue() >= 1.0) {
                    loader.setVisible(false); // HIDE LOADER when image is fully loaded
                    apodImage.setVisible(true);
                }
            });

            apodImage.setImage(image);
            if (img.getCopyright() != null) {
                cpRightPhotographer.setText(img.getCopyright().trim());
            } else {
                cpRightPhotographer.setText("APOD NASA API.");
            }
        } else if (apod instanceof VideoAPOD vid) {
            apodImage.setVisible(false);
            String ytEmbeddedVideo = vid.getUrl() + "&autoplay=1&mute=1&loop=1";

            apodYtVideo.getEngine().getLoadWorker().stateProperty().addListener((_, _, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    loader.setVisible(false);// HIDE LOADER when video is fully loaded
                    apodYtVideo.setVisible(true);
                } else if (newValue == Worker.State.FAILED) {
                    Image fallBackImage = new Image(getClass().getResource("/assets/broken-video.png").toExternalForm(), true);
                    apodImage.setImage(fallBackImage);
                    apodImage.setVisible(true);

                    loader.setVisible(false);
                    apodYtVideo.setVisible(false);

                    explanationAPOD.setText(FAIL_TEXT);
                    dateAPOD.setText(FAIL_TEXT);
                    cpRightPhotographer.setText(FAIL_TEXT);

                    titleAPOD.setText("Error loading media...");
                    titleAPOD.setStyle("-fx-text-fill: red; -fx-font-size: 15px");
                }
            });

            apodYtVideo.getEngine().load(ytEmbeddedVideo);

            cpRightPhotographer.setText("APOD NASA API.");
        } else {
            titleAPOD.setText("Unsupported media type.");
            apodImage.setVisible(false);
            apodYtVideo.setVisible(false);
            explanationAPOD.setVisible(false);
            cpRightPhotographer.setVisible(false);
            dateAPOD.setVisible(false);
        }
    }

    @FXML
    public void goHomeHandler(ActionEvent event) {
        MenuItem menuItem = (MenuItem) event.getSource();

        Stage stage = (Stage) menuItem.getParentPopup().getOwnerWindow();

        try {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(getClass().getResource("/fxml/root-apod.fxml"));

            FXMLLoader mainLoader = new FXMLLoader();
            mainLoader.setLocation(getClass().getResource("/fxml/main-apod.fxml"));

            BorderPane root = rootLoader.load();

            mainLoader.setControllerFactory(_ -> new MainApod(redisCacheService, gson, repository));
            AnchorPane homeAPOD = mainLoader.load();

            root.setCenter(homeAPOD);

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

    @FXML
    public void seeSavesHandler(ActionEvent event) {
        MenuItem menuItem = (MenuItem) event.getSource();

        Stage stage = (Stage) menuItem.getParentPopup().getOwnerWindow();

        try {
            FXMLLoader rootLoader = new FXMLLoader();
            rootLoader.setLocation(getClass().getResource("/fxml/root-apod.fxml"));

            FXMLLoader savesLoader = new FXMLLoader();
            savesLoader.setLocation(getClass().getResource("/fxml/saves-apod.fxml"));

            BorderPane root = rootLoader.load();

            savesLoader.setControllerFactory(_ -> new SavesApod(redisCacheService, gson, repository));
            AnchorPane savesAPOD = savesLoader.load();

            root.setCenter(savesAPOD);

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
}
