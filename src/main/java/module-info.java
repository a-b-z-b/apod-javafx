module org.apod {
    requires javafx.fxml;
    requires com.google.gson;
    requires javafx.web;
    requires java.net.http;
    exports org.apod;
    exports org.apod.controller;
    exports org.apod.model;
}
