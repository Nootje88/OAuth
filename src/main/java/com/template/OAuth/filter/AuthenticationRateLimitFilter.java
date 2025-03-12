package com.template.OAuth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.service.AuditService;
import com.template.OAuth.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Order(2)  // Apply after the main rate limiting filter
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // Track failed authentication attempts by IP
    private final ConcurrentMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    // IP addresses that are currently being throttled
    private final ConcurrentMap<String, Long> throttledIPs = new ConcurrentHashMap<>();

    public AuthenticationRateLimitFilter(RateLimitService rateLimitService,
                                         AuditService auditService,
                                         ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Only apply this filter to login attempts
        return !(path.startsWith("/auth/login") ||
                path.startsWith("/oauth2/authorization") ||
                path.startsWith("/login/oauth2"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIP(request);

        // Check if IP is currently throttled
        if (isIpThrottled(clientIp)) {
            long remainingSeconds = (throttledIPs.get(clientIp) - System.currentTimeMillis()) / 1000;

            if (remainingSeconds > 0) {
                auditService.logEvent(AuditEventType.ACCESS_DENIED,
                        "Authentication blocked due to previous failed attempts",
                        "IP: " + clientIp,
                        "THROTTLED");

                rejectRequest(response, "Authentication temporarily disabled",
                        "Too many failed attempts. Please try again in " + remainingSeconds + " seconds",
                        HttpStatus.TOO_MANY_REQUESTS);
                return;
            } else {
                // Throttling period is over
                throttledIPs.remove(clientIp);
                failedAttempts.remove(clientIp);
            }
        }

        // For authentication endpoints, apply stricter rate limiting
        Bucket bucket = rateLimitService.resolveBucketForAuthRequest(clientIp);
        long consumeTokens = 1;

        // If there are already failed attempts, consume more tokens (increasing the cost)
        Integer attempts = failedAttempts.get(clientIp);
        if (attempts != null && attempts > 0) {
            consumeTokens = Math.min(attempts + 1, 5);  // Max 5 tokens per request after failures
        }

        long remainingTokens = rateLimitService.checkRateLimit(bucket, consumeTokens);

        if (remainingTokens < 0) {
            auditService.logEvent(AuditEventType.ACCESS_DENIED,
                    "Rate limit exceeded for authentication endpoint",
                    "IP: " + clientIp,
                    "RATE_LIMITED");

            rejectRequest(response, "Too many authentication attempts",
                    "Please try again later", HttpStatus.TOO_MANY_REQUESTS);
            return;
        }

        // Use a custom response wrapper to detect authentication failures
        AuthenticationResponseWrapper responseWrapper = new AuthenticationResponseWrapper(response);

        filterChain.doFilter(request, responseWrapper);

        // If login was unsuccessful (based on response status), increment failed attempts
        if (responseWrapper.getStatus() == HttpStatus.UNAUTHORIZED.value() ||
                responseWrapper.getStatus() == HttpStatus.FORBIDDEN.value()) {

            recordFailedAttempt(clientIp);

            // Write the response back
            response.setStatus(responseWrapper.getStatus());
            response.getOutputStream().write(responseWrapper.getContentAsByteArray());
        } else {
            // Successful login, reset failed attempts
            failedAttempts.remove(clientIp);

            // Copy the response
            responseWrapper.copyBodyToResponse();
        }
    }

    private void recordFailedAttempt(String clientIp) {
        int attempts = failedAttempts.getOrDefault(clientIp, 0) + 1;
        failedAttempts.put(clientIp, attempts);

        // If too many failed attempts, start throttling
        if (attempts >= 5) {
            // Throttle for 30 minutes
            throttledIPs.put(clientIp, System.currentTimeMillis() + (30 * 60 * 1000));

            auditService.logEvent(AuditEventType.ACCESS_DENIED,
                    "IP throttled due to multiple failed authentication attempts",
                    "IP: " + clientIp + ", Attempts: " + attempts,
                    "THROTTLED");
        }
    }

    private boolean isIpThrottled(String clientIp) {
        return throttledIPs.containsKey(clientIp);
    }

    private void rejectRequest(HttpServletResponse response, String message, String details, HttpStatus status)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        errorResponse.put("details", details);
        errorResponse.put("status", String.valueOf(status.value()));

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}