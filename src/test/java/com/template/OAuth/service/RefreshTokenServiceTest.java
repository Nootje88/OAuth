package com.template.OAuth.service;

import com.template.OAuth.config.AppProperties;
import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.config.RefreshTokenProvider;
import com.template.OAuth.dto.RefreshTokenResponse;
import com.template.OAuth.entities.RefreshToken;
import com.template.OAuth.entities.User;
import com.template.OAuth.repositories.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenProvider refreshTokenProvider;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.Security security;

    @Mock
    private AppProperties.Security.Refresh refreshProps;

    @Mock
    private AppProperties.Security.Jwt jwtProps;

    @Mock
    private AppProperties.Security.Cookie cookieProps;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        // Set up test refresh token
        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setUser(testUser);
        testRefreshToken.setToken("valid-refresh-token");
        testRefreshToken.setExpiryDate(Instant.now().plusSeconds(3600)); // 1 hour in future

        // Mock AppProperties
        when(appProperties.getSecurity()).thenReturn(security);
        when(security.getRefresh()).thenReturn(refreshProps);
        when(security.getJwt()).thenReturn(jwtProps);
        when(security.getCookie()).thenReturn(cookieProps);
        when(refreshProps.getExpiration()).thenReturn(7 * 24 * 60 * 60 * 1000L); // 7 days
        when(jwtProps.getExpiration()).thenReturn(60 * 60 * 1000L); // 1 hour
        when(cookieProps.isSecure()).thenReturn(false);
        when(cookieProps.getSameSite()).thenReturn("Lax");
    }

    @Test
    void testGenerateRefreshToken_NewUser() {
        // Arrange
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(refreshTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        RefreshToken result = refreshTokenService.generateRefreshToken(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void testGenerateRefreshToken_ExistingToken() {
        // Arrange
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenProvider.generateRefreshToken()).thenReturn("updated-refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        RefreshToken result = refreshTokenService.generateRefreshToken(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void testRefreshToken_Valid() {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(testRefreshToken));
        when(jwtTokenProvider.generateToken(anyString())).thenReturn("new-jwt-token");
        when(refreshTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        var refreshResponse = refreshTokenService.refreshToken("valid-refresh-token", response);

        // Assert
        assertNotNull(refreshResponse);
        assertEquals("new-jwt-token", refreshResponse.getAccessToken());
        assertEquals("new-refresh-token", refreshResponse.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));

        // Check cookies were set
        assertTrue(response.getCookies().length >= 2);
    }

    @Test
    void testRefreshToken_Expired() {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Create expired token
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUser(testUser);
        expiredToken.setToken("expired-token");
        expiredToken.setExpiryDate(Instant.now().minusSeconds(3600)); // 1 hour in past

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));
        doNothing().when(refreshTokenRepository).delete(any(RefreshToken.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            refreshTokenService.refreshToken("expired-token", response);
        });

        verify(refreshTokenRepository, times(1)).delete(any(RefreshToken.class));
    }

    @Test
    void testRefreshToken_Invalid() {
        // Arrange
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            refreshTokenService.refreshToken("invalid-token", response);
        });
    }
}