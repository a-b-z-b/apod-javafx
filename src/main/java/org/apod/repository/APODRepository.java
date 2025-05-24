package org.apod.repository;

import org.apod.model.APOD;
import org.apod.model.ImageAPOD;
import org.apod.model.VideoAPOD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class APODRepository implements Repository<APOD> {
    private Connection connection;

    public APODRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public APOD findById(int id) {
        String sql = "SELECT * FROM APOD WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();

            if (rs.getString("media_type").equals("image")) {
                return new ImageAPOD(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("explanation"),
                        new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString("date")),
                        rs.getString("copyright"),
                        rs.getString("hdurl")
                );
            } else {
                return new VideoAPOD(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("explanation"),
                        new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString("date")),
                        rs.getString("url")
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        List<APOD> apods = new ArrayList<>();
        String sql = """
                        SELECT * FROM apod ORDER BY date DESC
                     """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)){
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                APOD apod = null;
                if(rs.getString("media_type").equals("video")) {
                    apod = new VideoAPOD(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("explanation"),
                            new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString("date")),
                            rs.getString("url")
                    );
                } else {
                    apod = new ImageAPOD(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("explanation"),
                            new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString("date")),
                            rs.getString("copyright"),
                            rs.getString("hdurl")
                    );
                }

                apods.add(apod);
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return apods;
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
