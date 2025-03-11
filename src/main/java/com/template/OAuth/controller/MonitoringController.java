package com.template.OAuth.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/monitoring")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Monitoring", description = "Application monitoring endpoints")
@SecurityRequirement(name = "cookie")
public class MonitoringController {

    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;

    @Autowired
    public MonitoringController(MeterRegistry meterRegistry, HealthEndpoint healthEndpoint) {
        this.meterRegistry = meterRegistry;
        this.healthEndpoint = healthEndpoint;
    }

    @Operation(summary = "Get application status",
            description = "Retrieves the overall application health status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @GetMapping("/status")
    public ResponseEntity<HealthComponent> getStatus() {
        return ResponseEntity.ok(healthEndpoint.health());
    }

    @Operation(summary = "Get authentication metrics",
            description = "Retrieves metrics related to authentication operations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @GetMapping("/metrics/auth")
    public ResponseEntity<Map<String, Object>> getAuthMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get authentication success count
        metrics.put("auth_success", getCounterValue("auth.success"));

        // Get authentication failure count
        metrics.put("auth_failure", getCounterValue("auth.failure"));

        // Get logout count
        metrics.put("logout", getCounterValue("auth.logout"));

        // Get registration count
        metrics.put("registrations", getCounterValue("user.registration"));

        // Get login timing statistics
        metrics.put("login_time_avg_ms", getTimerMean("auth.login.time"));

        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get HTTP request metrics",
            description = "Retrieves metrics related to HTTP requests")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @GetMapping("/metrics/http")
    public ResponseEntity<Map<String, Object>> getHttpMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get metrics for different status code groups
        metrics.put("status_2xx", getCounterValueWithTag("http.server.requests.status", "status_group", "2xx"));
        metrics.put("status_4xx", getCounterValueWithTag("http.server.requests.status", "status_group", "4xx"));
        metrics.put("status_5xx", getCounterValueWithTag("http.server.requests.status", "status_group", "5xx"));

        // Get endpoint timing metrics (top 10 slowest endpoints)
        Map<String, Double> endpointTimings = new TreeMap<>();
        Search.in(meterRegistry)
                .name("http.server.requests.custom")
                .timers()
                .stream()
                .collect(Collectors.groupingBy(
                        timer -> timer.getId().getTag("endpoint"),
                        Collectors.averagingDouble(timer -> timer.mean(TimeUnit.MILLISECONDS)) // Already in ms
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> endpointTimings.put(entry.getKey(), entry.getValue()));

        metrics.put("endpoint_timings_ms", endpointTimings);

        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get error metrics",
            description = "Retrieves metrics related to application errors")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @GetMapping("/metrics/errors")
    public ResponseEntity<Map<String, Object>> getErrorMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get total error count
        metrics.put("total_errors", getCounterValue("app.error"));

        // Get specific error counts
        metrics.put("unauthorized_errors", getCounterValueWithTag("app.error", "type", "unauthorized"));

        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get user activity metrics",
            description = "Retrieves metrics related to user activities")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @GetMapping("/metrics/user-activity")
    public ResponseEntity<Map<String, Object>> getUserActivityMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get profile update count
        metrics.put("profile_updates", getCounterValue("user.profile.update"));

        // Get role change count
        metrics.put("role_changes", getCounterValue("user.role.change"));

        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Get system metrics",
            description = "Retrieves metrics related to system resources")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @GetMapping("/metrics/system")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get JVM memory metrics
        metrics.put("jvm_memory_used_bytes", getGaugeValue("jvm.memory.used"));
        metrics.put("jvm_memory_max_bytes", getGaugeValue("jvm.memory.max"));

        // Get CPU metrics
        metrics.put("process_cpu_usage", getGaugeValue("process.cpu.usage"));
        metrics.put("system_cpu_usage", getGaugeValue("system.cpu.usage"));

        // Get thread metrics
        metrics.put("jvm_threads_live", getGaugeValue("jvm.threads.live"));
        metrics.put("jvm_threads_peak", getGaugeValue("jvm.threads.peak"));

        return ResponseEntity.ok(metrics);
    }

    // Helper methods for retrieving metrics - FIXED

    private double getCounterValue(String name) {
        Counter counter = Search.in(meterRegistry)
                .name(name)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }

    private double getCounterValueWithTag(String name, String tagKey, String tagValue) {
        Counter counter = Search.in(meterRegistry)
                .name(name)
                .tag(tagKey, tagValue)
                .counter();
        return counter != null ? counter.count() : 0.0;
    }

    private double getGaugeValue(String name) {
        Gauge gauge = Search.in(meterRegistry)
                .name(name)
                .gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    private double getTimerMean(String name) {
        Timer timer = Search.in(meterRegistry)
                .name(name)
                .timer();
        return timer != null ? timer.mean(TimeUnit.MILLISECONDS) : 0.0;
    }
}