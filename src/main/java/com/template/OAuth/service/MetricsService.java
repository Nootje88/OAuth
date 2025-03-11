package com.template.OAuth.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service for recording application-specific metrics
 */
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Authentication metrics
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Counter registrationCounter;
    private final Counter logoutCounter;

    // User metrics
    private final Counter profileUpdateCounter;
    private final Counter roleChangeCounter;

    // API metrics
    private final Timer apiRequestTimer;

    // Error metrics
    private final Counter errorCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.authSuccessCounter = Counter.builder("auth.success")
                .description("Number of successful authentications")
                .register(meterRegistry);

        this.authFailureCounter = Counter.builder("auth.failure")
                .description("Number of failed authentications")
                .register(meterRegistry);

        this.registrationCounter = Counter.builder("user.registration")
                .description("Number of user registrations")
                .register(meterRegistry);

        this.logoutCounter = Counter.builder("auth.logout")
                .description("Number of user logouts")
                .register(meterRegistry);

        this.profileUpdateCounter = Counter.builder("user.profile.update")
                .description("Number of profile updates")
                .register(meterRegistry);

        this.roleChangeCounter = Counter.builder("user.role.change")
                .description("Number of role changes")
                .register(meterRegistry);

        // Initialize timers
        this.apiRequestTimer = Timer.builder("api.request.duration")
                .description("API request duration")
                .register(meterRegistry);

        // Initialize error counter
        this.errorCounter = Counter.builder("app.error")
                .description("Number of application errors")
                .register(meterRegistry);
    }

    // Authentication metrics methods
    public void incrementAuthSuccess() {
        authSuccessCounter.increment();
    }

    public void incrementAuthFailure() {
        authFailureCounter.increment();
    }

    public void incrementRegistration() {
        registrationCounter.increment();
    }

    public void incrementLogout() {
        logoutCounter.increment();
    }

    // User metrics methods
    public void incrementProfileUpdate() {
        profileUpdateCounter.increment();
    }

    public void incrementRoleChange() {
        roleChangeCounter.increment();
    }

    // API metrics methods
    public <T> T recordApiRequestTime(String apiName, Supplier<T> supplier) {
        return Timer.builder("api.request.duration")
                .tag("api", apiName)
                .register(meterRegistry)
                .record(supplier);
    }

    public void recordApiRequestTime(String apiName, long timeInMs) {
        Timer.builder("api.request.duration")
                .tag("api", apiName)
                .register(meterRegistry)
                .record(timeInMs, TimeUnit.MILLISECONDS);
    }

    // Error metrics methods
    public void incrementError(String errorType) {
        Counter.builder("app.error")
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
    }

    // Custom counter method
    public void incrementCustomCounter(String name, String... tags) {
        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Tags must be provided as key-value pairs");
        }

        Counter.Builder counterBuilder = Counter.builder(name);
        for (int i = 0; i < tags.length; i += 2) {
            counterBuilder = counterBuilder.tag(tags[i], tags[i + 1]);
        }

        counterBuilder.register(meterRegistry).increment();
    }
}