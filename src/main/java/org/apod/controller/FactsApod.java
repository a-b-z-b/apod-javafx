package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.concurrent.Worker;
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
    @FXML
    public WebView loader;

    private final String APOD_KEY = "today:apod";
    private final String LOADER_KEY = "loader:apod";
    private final String FAIL_TEXT = "FAILED TO LOAD :/";

    private final int MAIN_APOD_WIDTH = 700;
    private final int MAIN_APOD_HEIGHT = 700;
    private final int SAVES_APOD_WIDTH = 950;
    private final int SAVES_APOD_HEIGHT = 900;

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

        String loaderMarkup = redisCacheService.get(LOADER_KEY);
        loader.getEngine().setUserStyleSheetLocation(getClass().getResource("/html/loader.css").toExternalForm());
        loader.getEngine().loadContent(loaderMarkup, "text/html");

        saveBtn.setVisible(false);

        renderApod(todayApodJson);
    }

    public void renderApod(String json) {

        if(json == null) {
            return;
        }

        Platform.runLater(() -> {
            APOD apod = gson.fromJson(json, new TypeToken<APOD>() {}.getType());
            theMainApod = apod;

            titleAPOD.setText(apod.getTitle());

            if (apod instanceof VideoAPOD) {
                apodImage.setVisible(false);

                String ytEmbeddedVideo = ((VideoAPOD) apod).getUrl() + "&autoplay=1&mute=1&loop=1";

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
                        saveBtn.setVisible(false);

                        explanationAPOD.setText(FAIL_TEXT);
                        dateAPOD.setText(FAIL_TEXT);
                        cpRightPhotographer.setText(FAIL_TEXT);

                        titleAPOD.setText("Error loading media...");
                        titleAPOD.setStyle("-fx-text-fill: red; -fx-font-size: 15px");
                    }
                });

                apodYtVideo.getEngine().load(ytEmbeddedVideo);

                cpRightPhotographer.setText("APOD NASA API.");
                explanationAPOD.setText(apod.getExplanation());
                dateAPOD.setText(new SimpleDateFormat("yyyy-MM-dd").format(apod.getDate()));
            } else if(apod instanceof ImageAPOD) {
                apodYtVideo.setVisible(false);

                if (((ImageAPOD) apod).getCopyright() != null) {
                    cpRightPhotographer.setText(((ImageAPOD) apod).getCopyright().trim());
                } else {
                    cpRightPhotographer.setText("APOD NASA API.");
                }

                Image image = new Image(((ImageAPOD) apod).getHdurl(), true);// true = background load

                image.errorProperty().addListener((_, _, isNowError) -> {
                    if(isNowError) {
                        Image fallBackImage = new Image(getClass().getResource("/assets/broken-3.jpg").toExternalForm(), true);
                        apodImage.setImage(fallBackImage);
                        apodImage.setVisible(true);

                        loader.setVisible(false);
                        apodYtVideo.setVisible(false);
                        saveBtn.setVisible(false);

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

                explanationAPOD.setText(apod.getExplanation().trim());
                dateAPOD.setText(new SimpleDateFormat("yyyy-MM-dd").format(apod.getDate()));
            } else {
                titleAPOD.setText("Unsupported media type.");
                apodImage.setVisible(false);
                apodYtVideo.setVisible(false);
                loader.setVisible(false);
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

            FXMLLoader mainLoader = new FXMLLoader();
            mainLoader.setLocation(getClass().getResource("/fxml/main-apod.fxml"));

            BorderPane root = rootLoader.load();

            mainLoader.setControllerFactory(_ -> new MainApod(redisCacheService, gson, apodRepository));
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

            savesLoader.setControllerFactory(_ -> new SavesApod(redisCacheService, gson, apodRepository));
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
