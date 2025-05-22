package org.apod.data;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public static Connection getConnection(String dbType) {
        try {
            switch (dbType){
                case "sqlite":
                    Class.forName("org.sqlite.JDBC");
                    return DriverManager.getConnection("jdbc:sqlite:data.db");
                case "test":
                    Class.forName("org.sqlite.JDBC");
                    return DriverManager.getConnection("jdbc:sqlite:data_test.db");
                default:
                    throw new IllegalArgumentException("Unsupported DB");
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
