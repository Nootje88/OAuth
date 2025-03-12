package com.template.OAuth.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test-specific configuration that replaces EnvConfig during tests
 */
@Configuration
@Profile("test")
public class TestEnvConfig {
    @PostConstruct
    public void init() {
        // Nothing needed here - test properties come from application-test.yaml
    }
}