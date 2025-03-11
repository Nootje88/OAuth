package com.template.OAuth.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
public class AuthHealthIndicator implements HealthIndicator {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public AuthHealthIndicator(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public Health health() {
        try {
            // Check if OAuth client registrations are available
            boolean googleAvailable = clientRegistrationRepository.findByRegistrationId("google") != null;

            if (googleAvailable) {
                return Health.up()
                        .withDetail("oauth_providers", "Available")
                        .withDetail("google", "Configured")
                        .build();
            } else {
                return Health.down()
                        .withDetail("oauth_providers", "Partially Available")
                        .withDetail("google", "Not Configured")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("oauth_providers", "Error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}