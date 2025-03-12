package com.template.OAuth.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private Cache.ValueWrapper valueWrapper;

    @Mock
    private Bandwidth authRateLimit;

    @Mock
    private Bandwidth sensitiveOperationsLimit;

    @InjectMocks
    private RateLimitService rateLimitService;

    private Bucket testBucket;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup cache mock
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // Create a real bucket for tests with a limit of 10 tokens
        testBucket = Bucket.builder()
                .addLimit(Bandwidth.simple(10, java.time.Duration.ofMinutes(1)))
                .build();
    }

    @Test
    void testResolveBucketForAuthRequest_NewBucket() {
        // Arrange
        when(cache.get("127.0.0.1")).thenReturn(null);
        when(cache.putIfAbsent(eq("127.0.0.1"), any(Bucket.class))).thenReturn(null);

        // Act
        Bucket bucket = rateLimitService.resolveBucketForAuthRequest("127.0.0.1");

        // Assert
        assertNotNull(bucket);
        verify(cache, times(1)).get("127.0.0.1");
    }

    @Test
    void testResolveBucketForAuthRequest_ExistingBucket() {
        // Arrange
        when(cache.get("127.0.0.1")).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(testBucket);

        // Act
        Bucket bucket = rateLimitService.resolveBucketForAuthRequest("127.0.0.1");

        // Assert
        assertNotNull(bucket);
        assertEquals(testBucket, bucket);
        verify(cache, times(1)).get("127.0.0.1");
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void testCheckRateLimit_Available() {
        // Arrange - bucket with available tokens
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.classic(10, io.github.bucket4j.Refill.greedy(10, java.time.Duration.ofMinutes(1))))
                .build();

        // Act
        long remainingTokens = rateLimitService.checkRateLimit(bucket, 1);

        // Assert
        assertTrue(remainingTokens >= 0);
    }

    @Test
    void testCheckRateLimit_Exhausted() {
        // Arrange - bucket with no tokens left
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.classic(1, io.github.bucket4j.Refill.greedy(1, java.time.Duration.ofMinutes(1))))
                .build();

        // Consume the only token
        bucket.tryConsume(1);

        // Act
        long remainingTokens = rateLimitService.checkRateLimit(bucket, 1);

        // Assert
        assertEquals(-1, remainingTokens);
    }

    @Test
    void testResolveBucketForSensitiveOperation() {
        // Arrange
        when(cache.get("127.0.0.1:sensitive")).thenReturn(null);

        // Act
        Bucket bucket = rateLimitService.resolveBucketForSensitiveOperation("127.0.0.1");

        // Assert
        assertNotNull(bucket);
        verify(cache, times(1)).get("127.0.0.1:sensitive");
    }
}