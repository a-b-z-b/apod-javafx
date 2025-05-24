package org.apod.model;

import java.util.Date;

public class ImageAPOD extends APOD {
    public String copyright;
    public String hdurl;

    public ImageAPOD() {
        this.media_type = "image";
    }

    public ImageAPOD(int id, String title, String explanation, Date date, String copyright, String hdurl) {
        super(id, title, explanation, "image", date);
        this.copyright = copyright;
        this.hdurl = hdurl;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getHdurl() {
        return hdurl;
    }

    public void setHdurl(String hdurl) {
        this.hdurl = hdurl;
    }

    @Override
    public String toString() {
        return "ImageAPOD{" +
                "copyright='" + copyright + '\'' +
                ", hdurl='" + hdurl + '\'' +
                ", title='" + title + '\'' +
                ", explanation='" + explanation + '\'' +
                ", media_type='" + media_type + '\'' +
                ", date=" + date +
                '}';
    }
}
