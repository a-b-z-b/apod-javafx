package org.apod.data;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static Connection connection;

    public static Connection getConnection(String dbType) {
        try {
            if (connection == null) {
                switch (dbType){
                    case "sqlite":
                        Class.forName("org.sqlite.JDBC");
                        connection = DriverManager.getConnection("jdbc:sqlite:data.db");
                        break;
                    case "test":
                        Class.forName("org.sqlite.JDBC");
                        return DriverManager.getConnection("jdbc:sqlite:data_test.db");
                    default:
                        throw new IllegalArgumentException("Unsupported DB");
                }
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
        return connection;
    }
}
