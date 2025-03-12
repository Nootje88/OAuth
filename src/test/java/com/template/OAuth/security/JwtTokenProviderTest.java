package com.template.OAuth.security;

import com.template.OAuth.config.AppProperties;
import com.template.OAuth.config.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.Security security;

    @Mock
    private AppProperties.Security.Jwt jwt;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up mocks
        when(appProperties.getSecurity()).thenReturn(security);
        when(security.getJwt()).thenReturn(jwt);
        when(jwt.getExpiration()).thenReturn(3600000L); // 1 hour

        // Set a test secret key
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "testSecretKeyWithAtLeast32CharactersForHS256SigningAlgorithm");

        // Create test UserDetails
        userDetails = new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
            }

            @Override
            public String getPassword() {
                return "password";
            }

            @Override
            public String getUsername() {
                return "test@example.com";
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
    }

    @Test
    void testGenerateAndValidateToken() {
        // Arrange & Act
        String token = jwtTokenProvider.generateToken("test@example.com");

        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("test@example.com", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void testGetTokenFromRequest() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = new Cookie[]{
                new Cookie("other", "value"),
                new Cookie("jwt", "test-token")
        };

        when(request.getCookies()).thenReturn(cookies);

        // Act
        Optional<String> token = jwtTokenProvider.getTokenFromRequest(request);

        // Assert
        assertTrue(token.isPresent());
        assertEquals("test-token", token.get());
    }

    @Test
    void testGetTokenFromRequest_NoCookies() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        // Act
        Optional<String> token = jwtTokenProvider.getTokenFromRequest(request);

        // Assert
        assertFalse(token.isPresent());
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Act
        boolean result = jwtTokenProvider.validateToken("invalid-token");

        // Assert
        assertFalse(result);
    }
}