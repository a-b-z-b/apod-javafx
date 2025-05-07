module org.apod {
    requires javafx.fxml;
    requires com.google.gson;
    requires javafx.web;
    requires java.net.http;
    requires redis.clients.jedis;
    requires org.xerial.sqlitejdbc;
    exports org.apod;
    exports org.apod.controller;
    exports org.apod.model;
    exports org.apod.service;
    exports org.apod.repository;
    exports org.apod.data;
}
