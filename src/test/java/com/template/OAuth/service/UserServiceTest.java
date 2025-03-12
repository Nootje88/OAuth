package com.template.OAuth.service;

import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.enums.Role;
import com.template.OAuth.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private OidcUser oidcUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPrimaryProvider(AuthProvider.GOOGLE);
        testUser.addRole(Role.USER);

        // Set up OidcUser
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "12345");
        claims.put("email", "test@example.com");
        claims.put("name", "Test User");
        claims.put("picture", "https://example.com/picture.jpg");

        OidcIdToken idToken = new OidcIdToken("token", null, null, claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);

        oidcUser = new DefaultOidcUser(
                Collections.emptyList(),
                idToken,
                userInfo
        );

        // Mock SecurityContextHolder
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");
    }

    @Test
    void testSaveUser_NewUser() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.saveUser(oidcUser, "google", "12345");

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals(AuthProvider.GOOGLE, result.getPrimaryProvider());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testSaveUser_ExistingUser() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.saveUser(oidcUser, "google", "12345");

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testGetCurrentUser() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testFindUserByEmail() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findUserByEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void testAssignRole() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.assignRole("test@example.com", Role.ADMIN);

        // Assert
        assertTrue(result.getRoles().contains(Role.ADMIN));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRemoveRole() {
        // Arrange
        testUser.addRole(Role.ADMIN);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.removeRole("test@example.com", Role.ADMIN);

        // Assert
        assertFalse(result.getRoles().contains(Role.ADMIN));
        verify(userRepository, times(1)).save(any(User.class));
    }
}