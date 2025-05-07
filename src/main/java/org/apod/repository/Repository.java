package org.apod.repository;

import java.util.List;

public interface Repository<T> {
    T findById(int id);
    void save(T t);
    List<T> findAll();
}
