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

    @Getter
    @Setter
    public static class Security {
        private Cookie cookie = new Cookie();
        private Jwt jwt = new Jwt();
        private Refresh refresh = new Refresh();

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
}