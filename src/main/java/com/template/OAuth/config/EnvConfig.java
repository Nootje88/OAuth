package com.template.OAuth.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;

@Configuration
public class EnvConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);

    @Autowired
    private Environment springEnv;

    @PostConstruct
    public void loadEnv() {
        // Determine which environment file to load based on active profile
        String activeProfile = getActiveProfile();
        String envFile = determineEnvFile(activeProfile);

        logger.info("Loading environment variables from: {}", envFile);

        // Load the environment file
        Dotenv dotenv = Dotenv.configure()
                .filename(envFile)
                .load();

        // Database Configuration
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
        System.setProperty("DB_URL", dotenv.get("DB_URL", "jdbc:mysql://localhost:3306/OAuthTemplate"));

        // JWT Configuration
        System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));

        // OAuth Client Secrets
        System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
        System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));

        // Optional OAuth providers
        setPropertyIfExists(dotenv, "SPOTIFY_CLIENT_ID");
        setPropertyIfExists(dotenv, "SPOTIFY_CLIENT_SECRET");
        setPropertyIfExists(dotenv, "APPLE_CLIENT_ID");
        setPropertyIfExists(dotenv, "APPLE_CLIENT_SECRET");
        setPropertyIfExists(dotenv, "SOUNDCLOUD_CLIENT_ID");
        setPropertyIfExists(dotenv, "SOUNDCLOUD_CLIENT_SECRET");

        // Email Configuration
        System.setProperty("EMAIL_HOST", dotenv.get("EMAIL_HOST", "smtp.gmail.com"));
        System.setProperty("EMAIL_PORT", dotenv.get("EMAIL_PORT", "587"));
        System.setProperty("EMAIL_USERNAME", dotenv.get("EMAIL_USERNAME"));
        System.setProperty("EMAIL_PASSWORD", dotenv.get("EMAIL_PASSWORD"));
        System.setProperty("EMAIL_FROM_ADDRESS", dotenv.get("EMAIL_FROM_ADDRESS", "no-reply@example.com"));
        System.setProperty("EMAIL_FROM_NAME", dotenv.get("EMAIL_FROM_NAME", "OAuth Template App"));
        System.setProperty("EMAIL_DEBUG", dotenv.get("EMAIL_DEBUG", "false"));

        // Application Configuration
        System.setProperty("APP_BASE_URL", dotenv.get("APP_BASE_URL", "http://localhost:3000"));
        System.setProperty("SUPPORT_EMAIL", dotenv.get("SUPPORT_EMAIL", "support@example.com"));
        System.setProperty("APP_NAME", dotenv.get("APP_NAME", "OAuth Template"));

        logger.info("Environment variables loaded successfully for profile: {}", activeProfile);
    }

    private String getActiveProfile() {
        String[] activeProfiles = springEnv.getActiveProfiles();

        if (activeProfiles.length > 0) {
            return activeProfiles[0];
        }

        // Default to 'dev' if no profile is specified
        return "dev";
    }

    private String determineEnvFile(String profile) {
        String envFileName = ".env." + profile;

        // Check if profile-specific .env file exists
        File profileEnvFile = new File(envFileName);
        if (profileEnvFile.exists()) {
            return envFileName;
        }

        // Fall back to default .env if profile-specific file doesn't exist
        logger.warn("Profile-specific env file {} not found. Falling back to default .env", envFileName);
        return ".env";
    }

    private void setPropertyIfExists(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }
}