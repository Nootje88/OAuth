package com.template.OAuth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        // allow comma-separated list in FRONTEND_URL (e.g., http://localhost:3000,http://127.0.0.1:3000)
        String[] origins = frontendUrl.split("\\s*,\\s*");

        registry.addMapping("/**")
                // Use allowedOrigins for exact origins. If you need wildcards, switch to allowedOriginPatterns().
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
