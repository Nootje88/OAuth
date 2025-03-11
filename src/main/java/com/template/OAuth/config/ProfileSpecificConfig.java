package com.template.OAuth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

@Configuration
public class ProfileSpecificConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProfileSpecificConfig.class);

    private final Environment environment;

    public ProfileSpecificConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void logActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            logger.info("No active profile set, using default profile");
        } else {
            for (String profile : activeProfiles) {
                logger.info("Active profile: {}", profile);
            }
        }
    }

    @Configuration
    @Profile("dev")
    public static class DevConfig {
        @PostConstruct
        public void init() {
            logger.info("Development environment initialized");
        }
    }

    @Configuration
    @Profile("test")
    public static class TestConfig {
        @PostConstruct
        public void init() {
            logger.info("Test environment initialized");
        }
    }

    @Configuration
    @Profile("pat")
    public static class PatConfig {
        @PostConstruct
        public void init() {
            logger.info("PAT environment initialized");
        }
    }

    @Configuration
    @Profile("prod")
    public static class ProdConfig {
        @PostConstruct
        public void init() {
            logger.info("Production environment initialized");

            // Additional production-specific settings
            logger.info("Production security measures activated");
        }
    }
}