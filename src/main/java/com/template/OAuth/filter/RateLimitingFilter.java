package com.template.OAuth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.OAuth.service.AuditService;
import com.template.OAuth.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    // Pattern for authentication endpoints
    private static final Pattern AUTH_ENDPOINTS = Pattern.compile("^/(auth|oauth2|login|refresh-token)/.*$");

    // Pattern for sensitive operations (admin, password changes, etc.)
    private static final Pattern SENSITIVE_ENDPOINTS = Pattern.compile("^/(api/admin|api/moderator|api/user/password)/.*$");

    public RateLimitingFilter(RateLimitService rateLimitService, AuditService auditService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIP(request);

        // Skip rate limiting for non-API requests
        if (shouldSkipRateLimiting(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket;
        long remainingTokens;

        // Apply different rate limits based on endpoint type
        if (AUTH_ENDPOINTS.matcher(path).matches()) {
            bucket = rateLimitService.resolveBucketForAuthRequest(clientIp);
            remainingTokens = rateLimitService.checkRateLimit(bucket, 1);

            if (remainingTokens < 0) {
                auditService.logEvent(com.template.OAuth.enums.AuditEventType.ACCESS_DENIED,
                        "Rate limit exceeded for authentication endpoint",
                        "IP: " + clientIp + ", Path: " + path,
                        "RATE_LIMITED");

                rejectRequest(response, "Too many authentication attempts",
                        "Please try again later", HttpStatus.TOO_MANY_REQUESTS);
                return;
            }
        }
        else if (SENSITIVE_ENDPOINTS.matcher(path).matches()) {
            bucket = rateLimitService.resolveBucketForSensitiveOperation(clientIp);
            remainingTokens = rateLimitService.checkRateLimit(bucket, 1);

            if (remainingTokens < 0) {
                auditService.logEvent(com.template.OAuth.enums.AuditEventType.ACCESS_DENIED,
                        "Rate limit exceeded for sensitive operation",
                        "IP: " + clientIp + ", Path: " + path,
                        "RATE_LIMITED");

                rejectRequest(response, "Rate limit exceeded for sensitive operations",
                        "Please try again later", HttpStatus.TOO_MANY_REQUESTS);
                return;
            }
        }
        else {
            // Default API rate limiting
            bucket = rateLimitService.resolveBucketForEndpoint(path);
            remainingTokens = rateLimitService.checkRateLimit(bucket, 1);

            if (remainingTokens < 0) {
                rejectRequest(response, "API rate limit exceeded",
                        "Please try again later", HttpStatus.TOO_MANY_REQUESTS);
                return;
            }
        }

        // Add rate limit headers
        response.addHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipRateLimiting(String path) {
        // Skip static resources, Swagger UI, error pages
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/favicon.ico") ||
                path.startsWith("/error") ||
                path.equals("/");
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