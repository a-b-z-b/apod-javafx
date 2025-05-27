package org.apod.util;

import javafx.scene.web.WebView;
import org.apod.service.AbstractCacheService;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UIHelper {
    public static void spinLoader(AbstractCacheService cacheService, WebView loader, String LOADER_KEY, int APOD_TTL) {
        String loaderMarkup = null;
        if(cacheService.get(LOADER_KEY) != null) {
            loaderMarkup = cacheService.get(LOADER_KEY);
        } else {
            loaderMarkup = loadHtmlToString("loader.html");
            cacheService.set(LOADER_KEY, loaderMarkup, APOD_TTL);
        }

        loader.getEngine().setUserStyleSheetLocation(UIHelper.class.getResource("/html/loader.css").toExternalForm());
        loader.getEngine().loadContent(loaderMarkup, "text/html");
    }

    protected static String loadHtmlToString(String path) {
        try {
            URL markupPath = UIHelper.class.getResource("/html/" + path);
            if (markupPath == null) {
                throw new RuntimeException("Could not find html file: " + path);
            }
            return Files.readString(Paths.get(markupPath.toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
