package org.apod.repository;

import java.sql.Connection;

public class RepositoryFactory {
    public static <T> Repository<T> createRepository(String dbType, Connection connection) {
        switch (dbType) {
            case "sqlite":
                return new SQLiteRepository<>(connection);
            default:
                throw new IllegalArgumentException("Unsupported DB type");
        }
    }
}
