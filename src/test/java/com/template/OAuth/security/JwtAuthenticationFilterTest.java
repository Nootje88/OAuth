package com.template.OAuth.security;

import com.template.OAuth.config.JwtAuthenticationFilter;
import com.template.OAuth.config.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Reset security context
        SecurityContextHolder.clearContext();

        // Create test UserDetails
        userDetails = new UserPrincipal(
                "test@example.com",
                "encodedPassword",
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void testDoFilter_ValidToken() throws ServletException, IOException {
        // Arrange
        when(jwtTokenProvider.getTokenFromRequest(request)).thenReturn(Optional.of("valid-token"));
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("valid-token")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_NoToken() throws ServletException, IOException {
        // Arrange
        when(jwtTokenProvider.getTokenFromRequest(request)).thenReturn(Optional.empty());

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_InvalidToken() throws ServletException, IOException {
        // Arrange
        when(jwtTokenProvider.getTokenFromRequest(request)).thenReturn(Optional.of("invalid-token"));
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_EmailNotFound() throws ServletException, IOException {
        // Arrange
        when(jwtTokenProvider.getTokenFromRequest(request)).thenReturn(Optional.of("valid-token"));
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("valid-token")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_UserNotFound() throws ServletException, IOException {
        // Arrange
        when(jwtTokenProvider.getTokenFromRequest(request)).thenReturn(Optional.of("valid-token"));
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("valid-token")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername(anyString())).thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        // Expect the exception to be thrown
        assertThrows(RuntimeException.class, () -> {
            jwtAuthenticationFilter.doFilter(request, response, filterChain);
        });

        // Security context should remain empty
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}