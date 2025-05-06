package org.apod.model;

import java.util.Date;

public class VideoAPOD extends APOD {
    public String url;

    public VideoAPOD() {}

    public VideoAPOD(String title, String explanation, String media_type, Date date, String url) {
        super(title, explanation, media_type, date);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "VideoAPOD{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", explanation='" + explanation + '\'' +
                ", media_type='" + media_type + '\'' +
                ", date=" + date +
                '}';
    }
}
