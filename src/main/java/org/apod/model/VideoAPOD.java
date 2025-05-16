package org.apod.model;

import java.util.Date;

public class VideoAPOD extends APOD {
    public String url;

    public VideoAPOD() {
        this.media_type = "video";
    }

    public VideoAPOD(String title, String explanation, Date date, String url) {
        super(title, explanation, "video", date);
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
