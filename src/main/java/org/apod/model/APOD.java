package org.apod.model;

import java.util.Date;

public class APOD {
    public String title;
    public String explanation;
    public String media_type;
    public Date date;

    public APOD() {
    }

    public APOD(String title, String explanation, String media_type, Date date) {
        this.title = title;
        this.explanation = explanation;
        this.media_type = media_type;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getMedia_type() {
        return media_type;
    }

    public void setMedia_type(String media_type) {
        this.media_type = media_type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "APOD{" +
                "title='" + title + '\'' +
                ", explanation='" + explanation + '\'' +
                ", media_type='" + media_type + '\'' +
                ", date=" + date +
                '}';
    }
}
