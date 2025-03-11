package com.template.OAuth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration specific to Actuator endpoints
 */
@Configuration
public class ActuatorSecurityConfig {

    /**
     * Configure security specifically for Actuator endpoints
     */
    @Bean
    @Order(1) // Higher priority than the main security config
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(new AntPathRequestMatcher("/management/**"))
                .authorizeHttpRequests(authorize -> authorize
                        // Publicly accessible endpoints
                        .requestMatchers("/management/health", "/management/info").permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/management/health/**").hasRole("ADMIN")
                        .requestMatchers("/management/env/**").hasRole("ADMIN")
                        .requestMatchers("/management/metrics/**").hasRole("ADMIN")
                        .requestMatchers("/management/prometheus/**").hasRole("ADMIN")
                        .requestMatchers("/management/loggers/**").hasRole("ADMIN")
                        .requestMatchers("/management/mappings/**").hasRole("ADMIN")

                        // Default deny
                        .anyRequest().hasRole("ADMIN")
                )
                // Reuse the main security configuration
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configure(http));

        return http.build();
    }
}