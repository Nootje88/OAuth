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
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Environment springEnv;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private OidcUser oidcUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set admin emails using reflection
        ReflectionTestUtils.setField(userService, "adminEmails", "admin@example.com,test-admin@example.com");

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

        // Mock active profiles for test environment
        when(springEnv.getActiveProfiles()).thenReturn(new String[]{"test"});

        // Mock SecurityContextHolder
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");
        when(authentication.getPrincipal()).thenReturn("test@example.com");
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
    void testSaveUser_AdminUser() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "12345");
        claims.put("email", "admin@example.com");  // This is in the admin emails list
        claims.put("name", "Admin User");
        claims.put("picture", "https://example.com/admin.jpg");

        OidcIdToken idToken = new OidcIdToken("token", null, null, claims);
        OidcUserInfo userInfo = new OidcUserInfo(claims);

        OidcUser adminOidcUser = new DefaultOidcUser(
                Collections.emptyList(),
                idToken,
                userInfo
        );

        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin User");
        adminUser.setPrimaryProvider(AuthProvider.GOOGLE);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            adminUser.setRoles(savedUser.getRoles());
            return adminUser;
        });

        // Act
        User result = userService.saveUser(adminOidcUser, "google", "12345");

        // Assert
        assertNotNull(result);
        assertEquals("admin@example.com", result.getEmail());
        assertTrue(result.getRoles().contains(Role.ADMIN), "User should have ADMIN role");
        assertTrue(result.getRoles().contains(Role.USER), "User should have USER role");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testGetCurrentUser() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Mock active profiles for test environment
        when(springEnv.getActiveProfiles()).thenReturn(new String[]{"test"});

        // Set up authentication to be a UsernamePasswordAuthenticationToken for test path
        when(authentication.getName()).thenReturn("test@example.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication instanceof UsernamePasswordAuthenticationToken).thenReturn(true);
        SecurityContextHolder.setContext(securityContext);

        // Act
        User result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findByEmail("test@example.com");
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
        verify(userRepository, times(1)).findByEmail("test@example.com");
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

    @Test
    void testFindUserByEmail_NotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.findUserByEmail("nonexistent@example.com");
        });

        assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void testGetCurrentUser_TestEnvironment_CreatesFakeUser() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Mock active profiles for test environment
        when(springEnv.getActiveProfiles()).thenReturn(new String[]{"test"});

        // Set up authentication to be a UsernamePasswordAuthenticationToken for test path
        when(authentication.getName()).thenReturn("notfound@example.com");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication instanceof UsernamePasswordAuthenticationToken).thenReturn(true);
        SecurityContextHolder.setContext(securityContext);

        // Act
        User result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals("notfound@example.com", result.getEmail());
        assertEquals(999L, result.getId()); // The fake user ID
        assertEquals("Test User", result.getName()); // Default test user name
        verify(userRepository, times(1)).findByEmail("notfound@example.com");
    }
}