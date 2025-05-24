package org.apod.layout;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class APODCard extends StackPane {
    public APODCard() {
        setStyle("-fx-border-color: #0abc10; -fx-cursor:hand;");
    }

    /** Called when the real node is ready to show. */
    public void setContent(Node content) {
        getChildren().setAll(content);
    }

    /** Called if loading failed. */
    public void setFallback(String mediaType) {
        String fallBackAsset = null;

        if (mediaType.equals("image")) {
            fallBackAsset = APODCard.class.getResource("/assets/broken-file.jpg").toExternalForm();
        } else {
            fallBackAsset = APODCard.class.getResource("/assets/broken-video.png").toExternalForm();
        }

        ImageView iv = new ImageView(new Image(fallBackAsset));
        iv.setPreserveRatio(true);
        iv.setFitWidth(getPrefWidth());
        iv.setFitHeight(getPrefHeight());
        Platform.runLater(() -> getChildren().setAll(iv));
    }
}
