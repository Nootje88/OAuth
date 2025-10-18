package com.template.OAuth.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull; // <-- added
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Filter that records metrics for all HTTP requests
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class MetricsFilter extends OncePerRequestFilter {

    private final MeterRegistry meterRegistry;

    // Pattern to match API endpoints but exclude static resources and actuator endpoints
    private static final Pattern API_PATTERN = Pattern.compile("^/(api|auth|oauth2|refresh-token).*");
    private static final Pattern EXCLUDED_PATTERN = Pattern.compile("^/(management|swagger-ui|v3/api-docs).*");

    public MetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only record metrics for API requests
        if (API_PATTERN.matcher(path).matches() && !EXCLUDED_PATTERN.matcher(path).matches()) {
            Instant start = Instant.now();

            try {
                // Proceed with the request
                filterChain.doFilter(request, response);
            } finally {
                // Record request duration
                Duration duration = Duration.between(start, Instant.now());

                // Create metrics tags
                String method = request.getMethod();
                String endpoint = sanitizeEndpoint(path);
                String status = String.valueOf(response.getStatus());
                String statusGroup = status.charAt(0) + "xx";

                // Record the metric
                Timer.builder("http.server.requests.custom")
                        .tag("method", method)
                        .tag("endpoint", endpoint)
                        .tag("status", status)
                        .tag("status_group", statusGroup)
                        .register(meterRegistry)
                        .record(duration);

                // Count for status groups
                meterRegistry.counter("http.server.requests.status",
                                "method", method,
                                "status_group", statusGroup)
                        .increment();
            }
        } else {
            // Skip metric recording for non-API requests
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Sanitize and normalize the endpoint path to avoid high cardinality metrics
     */
    private String sanitizeEndpoint(String path) {
        // Replace numeric IDs with {id} to reduce cardinality
        String normalized = path.replaceAll("/\\d+", "/{id}");

        // Truncate to avoid very long paths
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50) + "...";
        }

        return normalized;
    }
}
