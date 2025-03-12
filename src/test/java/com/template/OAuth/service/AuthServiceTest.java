package com.template.OAuth.service;

import com.template.OAuth.config.AppProperties;
import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.dto.EmailLoginRequest;
import com.template.OAuth.dto.EmailRegistrationRequest;
import com.template.OAuth.entities.User;
import com.template.OAuth.repositories.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditService auditService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.Security security;

    @Mock
    private AppProperties.Security.Verification verification;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private EmailRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("encodedPassword");

        // Set up registration request
        registrationRequest = new EmailRegistrationRequest();
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setName("Test User");
        registrationRequest.setPassword("Password123!");

        // Mock AppProperties
        when(appProperties.getSecurity()).thenReturn(security);
        when(security.getVerification()).thenReturn(verification);
        when(verification.getExpirationHours()).thenReturn(24);
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        User result = authService.registerUser(registrationRequest);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.registerUser(registrationRequest);
        });

        assertEquals("Email is already in use", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testVerifyEmail_Success() {
        // Arrange
        testUser.setVerificationToken("valid-token");
        testUser.setVerificationTokenExpiry(Instant.now().plusSeconds(3600)); // 1 hour in future
        testUser.setEnabled(false);

        when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        // Act
        boolean result = authService.verifyEmail("valid-token");

        // Assert
        assertTrue(result);
        assertTrue(testUser.isEnabled());
        assertNull(testUser.getVerificationToken());
        assertNull(testUser.getVerificationTokenExpiry());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void testVerifyEmail_ExpiredToken() {
        // Arrange
        testUser.setVerificationToken("expired-token");
        testUser.setVerificationTokenExpiry(Instant.now().minusSeconds(3600)); // 1 hour in past
        testUser.setEnabled(false);

        when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyEmail("expired-token");
        });

        assertEquals("Verification token has expired", exception.getMessage());
        assertFalse(testUser.isEnabled());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAuthenticateAndGenerateTokens() {
        // Arrange
        EmailLoginRequest loginRequest = new EmailLoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password123!");

        // Create a mock UserPrincipal for the authentication
        com.template.OAuth.security.UserPrincipal userPrincipal =
                new com.template.OAuth.security.UserPrincipal(
                        "test@example.com",
                        "encodedPassword",
                        true,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(anyString())).thenReturn("jwt-token");

        // Act
        authService.authenticateAndGenerateTokens(loginRequest, servletResponse);

        // Assert
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, times(1)).generateToken(anyString());
        verify(refreshTokenService, times(1)).generateRefreshToken(any(User.class));
        verify(servletResponse, times(2)).addCookie(any());
    }
}