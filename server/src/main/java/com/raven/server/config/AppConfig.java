package com.raven.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static AppConfig instance;
    private final Properties properties = new Properties();

    private AppConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                log.warn("application.properties not found in classpath. Using default settings.");
            } else {
                properties.load(input);
                log.info("Successfully loaded application.properties from resources.");
            }
        } catch (Exception e) {
            log.error("Failed to load application.properties", e);
        }
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer property for key: {}. Using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
}
