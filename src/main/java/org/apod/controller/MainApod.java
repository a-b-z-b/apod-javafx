package org.apod.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
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
import javafx.util.Duration;
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

    private final int FACTS_BTN_LAYOUT_X = 310;
    private final int FACTS_BTN_LAYOUT_Y = 542;

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
        PauseTransition timeout = new PauseTransition(Duration.seconds(20));

        timeout.setOnFinished(_ -> {
            if (
                    apodYtVideo.getEngine().getLoadWorker().getProgress() < 1.0
                    || todayApod.getImage() == null
                    || todayApod.getImage().isError()
                    || todayApod.getImage().getProgress() < 1.0
            ) {
                System.out.println("Timeout: Failed to load media.");
                // TODO: fallback UI
                Image fallBackImage = new Image(getClass().getResource("/assets/broken-1.jpg").toExternalForm(), true);
                todayApod.setImage(fallBackImage);
                todayApod.setVisible(true);

                loader.setVisible(false);
                apodYtVideo.setVisible(false);

                apodTitle.setText("Error loading media...\nCheck your internet connection.");
                apodTitle.setStyle("-fx-text-fill: red; -fx-font-size: 15px");
            }
        });

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
        fullscreenBtn.setVisible(false);

        if(todayApodJson != null) {
            System.out.println("CACHE HIT -> todayApodJson: " + todayApodJson);
            renderApod(todayApodJson, timeout);
        } else {
            // we must run timeout to handle case being offline and data not-cached hence http request fails.
            timeout.play();
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
                        renderApod(json, timeout);
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        }
    }

    public void renderApod(String json, PauseTransition timeout) {
        Platform.runLater(() -> {
            APOD apod = null;
            try {
                apod = gson.fromJson(json, new TypeToken<APOD>() {}.getType());
            } catch (Exception e) {
                // Handle case when endpoint returns a media_type json-field other than image/video
                // hence gson parser throws a JsonParseException exception.
                timeout.stop();
                apodTitle.setText("Error loading media...\nUnsupported media type.");
                Image fallBackImage = new Image(getClass().getResource("/assets/broken-2.jpg").toExternalForm(), true);
                todayApod.setImage(fallBackImage);
                todayApod.setVisible(true);

                loader.setVisible(false);
                apodYtVideo.setVisible(false);
                fullscreenBtn.setVisible(false);
                factsBtn.setVisible(false);
                saveBtn.setVisible(false);

                apodTitle.setStyle("-fx-text-fill: red; -fx-font-size: 15px");
                System.out.println("EXCEPTION :" + e.getMessage());
                return;
            }

            theMainApod = apod;

            apodTitle.setText(apod.getTitle());

            if (apod instanceof VideoAPOD) {
                fullscreenBtn.setVisible(true);
                todayApod.setVisible(false);
                fullscreenBtn.setVisible(false);
                String ytEmbeddedVideo = ((VideoAPOD) apod).getUrl() + "&autoplay=1&mute=1&loop=1";

                timeout.play();
                apodYtVideo.getEngine().getLoadWorker().stateProperty().addListener((_, _, newValue) -> {
                    if (newValue == Worker.State.SUCCEEDED) {
                        timeout.stop();
                        loader.setVisible(false);// HIDE LOADER when video is fully loaded
                        apodYtVideo.setVisible(true);
                    } else if (newValue == Worker.State.FAILED){
                        Image fallBackImage = new Image(getClass().getResource("/assets/broken-video.png").toExternalForm(), true);
                        todayApod.setImage(fallBackImage);
                        todayApod.setVisible(true);

                        apodYtVideo.setVisible(false);
                        fullscreenBtn.setVisible(false);
                        factsBtn.setVisible(false);

                        apodTitle.setText("Error loading media...");
                        apodTitle.setStyle("-fx-text-fill: red; -fx-font-size: 15px");
                    }
                });

                apodYtVideo.getEngine().load(ytEmbeddedVideo);
            } else if(apod instanceof ImageAPOD) {
                apodYtVideo.setVisible(false);

                Image image = new Image(((ImageAPOD) apod).getHdurl(), true);// true = background load

                image.errorProperty().addListener((_, _, isNowError) -> {
                    if(isNowError) {
                        Image fallBackImage = new Image(getClass().getResource("/assets/broken-1.jpg").toExternalForm(), true);
                        todayApod.setImage(fallBackImage);
                        todayApod.setVisible(true);

                        apodYtVideo.setVisible(false);
                        fullscreenBtn.setVisible(false);
                        factsBtn.setVisible(false);
                        saveBtn.setVisible(false);

                        apodTitle.setText("Error loading media...");
                        apodTitle.setStyle("-fx-text-fill: red; -fx-font-size: 15px");
                    }
                });

                timeout.play();
                image.progressProperty().addListener((_, _, newValue) -> {
                    if (newValue.doubleValue() >= 1.0) {
                        timeout.stop();
                        loader.setVisible(false);// HIDE LOADER when image is fully loaded
                        todayApod.setVisible(true);
                    }
                });

                todayApod.setImage(image);
                fullscreenBtn.setVisible(true);
            } else {
                apodTitle.setText("Unsupported media type.");
                todayApod.setVisible(false);
                apodYtVideo.setVisible(false);
                loader.setVisible(false);
            }

            if(!repository.existsByDate(theMainApod.getDate())){
                saveBtn.setVisible(true);
            }

            factsBtn.setVisible(true);
            if (!saveBtn.isVisible()) {
                factsBtn.setLayoutX(FACTS_BTN_LAYOUT_X);
                factsBtn.setLayoutY(FACTS_BTN_LAYOUT_Y);
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

            factsLoader.setControllerFactory(_ -> new FactsApod(redisCacheService, gson, repository));
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
            factsBtn.setLayoutX(FACTS_BTN_LAYOUT_X);
            factsBtn.setLayoutY(FACTS_BTN_LAYOUT_Y);

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
