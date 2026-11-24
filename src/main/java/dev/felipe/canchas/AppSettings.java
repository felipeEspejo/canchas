package dev.felipe.canchas;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Lee la configuración: primero la variable de entorno (por ejemplo DB_URL) y, si no existe,
 * la propiedad de application.properties (por ejemplo app.db.url).
 */
final class AppSettings {
    private final Properties file = new Properties();

    AppSettings() {
        try (InputStream in = AppSettings.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                file.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer application.properties", e);
        }
    }

    String get(String envName, String propertyName, String fallback) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = file.getProperty(propertyName);
        return fromFile == null || fromFile.isBlank() ? fallback : fromFile.trim();
    }

    int getInt(String envName, String propertyName, int fallback) {
        return Integer.parseInt(get(envName, propertyName, String.valueOf(fallback)));
    }
}
