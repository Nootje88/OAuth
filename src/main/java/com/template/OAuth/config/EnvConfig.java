package com.template.OAuth.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {
        Dotenv dotenv = Dotenv.load();

        // Database Credentials
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));

        // OAuth Client Secrets
        System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
        System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));

        System.setProperty("SPOTIFY_CLIENT_ID", dotenv.get("SPOTIFY_CLIENT_ID"));
        System.setProperty("SPOTIFY_CLIENT_SECRET", dotenv.get("SPOTIFY_CLIENT_SECRET"));

        System.setProperty("APPLE_CLIENT_ID", dotenv.get("APPLE_CLIENT_ID"));
        System.setProperty("APPLE_CLIENT_SECRET", dotenv.get("APPLE_CLIENT_SECRET"));

        System.setProperty("SOUNDCLOUD_CLIENT_ID", dotenv.get("SOUNDCLOUD_CLIENT_ID"));
        System.setProperty("SOUNDCLOUD_CLIENT_SECRET", dotenv.get("SOUNDCLOUD_CLIENT_SECRET"));

        // JWT Secret Key
        System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));
    }
}
