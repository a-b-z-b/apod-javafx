package org.apod.data;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

public class MigrationsRunner {
    public static void runMigrations(Connection conn, String... sqlFilePaths) {
        for (String sqlFilePath : sqlFilePaths) {
            try {
                URL resourceUrl = MigrationsRunner.class.getResource(sqlFilePath);
                System.out.println("RESOURCE URL: " + resourceUrl);
                if (resourceUrl == null) {
                    throw new RuntimeException("Resource not found: " + sqlFilePath);
                }

                String sql = Files.readString(Path.of(resourceUrl.toURI()));

                String[] sqls = sql.split("(?m);\\s*");

                for(String s: sqls) {
                    try(Statement stmt = conn.createStatement()) {
                        stmt.execute(s.trim());
                    }
                }

                System.out.println("Executed migration: " + sqlFilePath);

            } catch (Exception e) {
                throw new RuntimeException("Failed to execute migration: " + sqlFilePath, e);
            }
        }
    }
}
