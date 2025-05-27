package org.apod.controller;

import com.google.gson.Gson;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;
import org.apod.repository.APODRepository;
import org.apod.service.AbstractCacheService;
import org.apod.util.FXHelper;
import org.apod.util.UIHelper;

import java.text.SimpleDateFormat;

public class DetailsApod {
    private final int APOD_TTL = 3600;
    private final String LOADER_KEY = "loader:apod";
    private final String FAIL_TEXT = "FAILED TO LOAD :/";

    private Gson gson;
    private AbstractCacheService cacheService;
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

    public DetailsApod(AbstractCacheService cacheService, Gson gson, APODRepository repository, APOD apodData) {
        this.cacheService = cacheService;
        this.gson = gson;
        this.repository = repository;
        this.apod = apodData;
    }

    @FXML
    public void initialize() {
        UIHelper.spinLoader(cacheService, loader, LOADER_KEY, APOD_TTL);

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
        FXHelper.switchToHome(event, cacheService, gson, repository);
    }

    @FXML
    public void seeSavesHandler(ActionEvent event) {
        FXHelper.switchToSaves(event, cacheService, gson, repository);
    }
}
