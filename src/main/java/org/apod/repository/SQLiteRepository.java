package org.apod.repository;


import java.sql.Connection;
import java.util.List;

public class SQLiteRepository<T> implements Repository<T> {
    private Connection connection;

    public SQLiteRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public T findById(int id) {
        return null;
    }

    @Override
    public void save(T obj) {

    }

    @Override
    public List<T> findAll() {
        return List.of();
    }
}
