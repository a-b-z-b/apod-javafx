package org.apod.repository;

import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class APODRepository implements Repository<APOD> {
    private Connection connection;

    public APODRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public APOD findById(int id) {
        return null;
    }

    @Override
    public void save(APOD apod) {
        String query = """
                            INSERT INTO apod (title, date, explanation, media_type, copyright, hdurl, url)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                       """;
        try(PreparedStatement statement = connection.prepareStatement(query);) {

            statement.setString(1, apod.getTitle());
            statement.setString(2, new SimpleDateFormat("yyyy-MM-dd").format(apod.getDate()));
            statement.setString(3, apod.getExplanation());
            statement.setString(4, apod.getMedia_type());

            if (apod instanceof ImageAPOD) {
                statement.setString(5, ((ImageAPOD) apod).getCopyright());
                statement.setString(6, ((ImageAPOD) apod).getHdurl());
            }

            if (apod instanceof VideoAPOD) {
                statement.setString(7, ((VideoAPOD) apod).getUrl());
            }

            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<APOD> findAll() {
        return List.of();
    }

    public boolean existsByDate(Date date) {
        String sql = "SELECT 1 FROM apod WHERE date = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String strDate = new SimpleDateFormat("yyyy-MM-dd").format(date);

            statement.setString(1, strDate);

            ResultSet rs = statement.executeQuery();

            return rs.next();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
