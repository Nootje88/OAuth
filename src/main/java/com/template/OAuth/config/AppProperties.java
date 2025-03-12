package com.template.OAuth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Security security = new Security();
    private Profile profile = new Profile();
    private Cors cors = new Cors();
    private Email email = new Email();
    private Application application = new Application();

    @Getter
    @Setter
    public static class Security {
        private Cookie cookie = new Cookie();
        private Jwt jwt = new Jwt();
        private Refresh refresh = new Refresh();
        private RateLimiting rateLimiting = new RateLimiting();
        private Verification verification = new Verification();
        private PasswordReset passwordReset = new PasswordReset();

        @Getter
        @Setter
        public static class Cookie {
            private boolean secure = false;
            private String sameSite = "lax";
        }

        @Getter
        @Setter
        public static class Jwt {
            private long expiration = 3600000; // 1 hour in milliseconds
        }

        @Getter
        @Setter
        public static class Refresh {
            private long expiration = 604800000; // 7 days in milliseconds
        }

        @Getter
        @Setter
        public static class RateLimiting {
            private boolean enabled = true;
            private int defaultLimit = 100;
            private int authLimit = 10;
            private int sensitiveLimit = 3;
            private int blockDurationMinutes = 30;
            private int maxFailedAttempts = 5;
        }

        @Getter
        @Setter
        public static class Verification {
            private int expirationHours = 24;
        }

        @Getter
        @Setter
        public static class PasswordReset {
            private int expirationHours = 1;
        }
    }

    @Getter
    @Setter
    public static class Profile {
        private String uploadDir = "uploads/profiles";
        private int maxBioLength = 500;
        private String defaultTheme = "SYSTEM";
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }

    @Getter
    @Setter
    public static class Email {
        private String host = "smtp.gmail.com";
        private int port = 587;
        private String username;
        private String password;
        private String fromAddress;
        private String fromName = "OAuth Template App";
        private boolean debug = false;
    }

    @Getter
    @Setter
    public static class Application {
        private String baseUrl = "http://localhost:3000";
        private String supportEmail = "support@example.com";
        private String name = "OAuth Template";
    }
}