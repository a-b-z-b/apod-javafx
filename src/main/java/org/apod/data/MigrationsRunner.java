package org.apod.data;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

public class MigrationsRunner {
    public static void runMigrations(Connection conn, String... sqlFilePaths) {
        for (String sqlFilePath : sqlFilePaths) {
            try {
                URL resourceUrl = MigrationsRunner.class.getResource(sqlFilePath);
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

    public static void runTestMigrations(Connection conn, String... sqlTestFilePaths) {
        ClassLoader classLoader = MigrationsRunner.class.getClassLoader();

        for (String sqlFilePath : sqlTestFilePaths) {
            try (InputStream inputStream = classLoader.getResourceAsStream(sqlFilePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Resource not found: " + sqlFilePath);
                }

                String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                String[] sqls = sql.split("(?m);\\s*");

                for (String s : sqls) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(trimmed);
                        }
                    }
                }

                System.out.println("Executed migration: " + sqlFilePath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute migration: " + sqlFilePath, e);
            }
        }
    }
}
