package com.template.OAuth.service;

import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.enums.NotificationType;
import com.template.OAuth.enums.Role;
import com.template.OAuth.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User saveUser(OidcUser oidcUser, String providerName, String providerId) {
        String email = oidcUser.getEmail();
        Optional<User> existingUser = userRepository.findByEmail(email);

        // Determine the auth provider based on the issuer URL
        AuthProvider authProvider = determineAuthProvider(providerName);

        User user;
        if (existingUser.isPresent()) {
            // Update existing user
            user = existingUser.get();
            // Update profile info if needed
            user.setName(oidcUser.getFullName());
            user.setPicture(oidcUser.getPicture());

            // Record login
            user.recordLogin();
        } else {
            // Create new user
            user = new User();
            user.setName(oidcUser.getFullName());
            user.setEmail(email);
            user.setPicture(oidcUser.getPicture());
            user.setPrimaryProvider(authProvider);

            // Assign default USER role to all new users
            user.addRole(Role.USER);

            // Example: Assign ADMIN role to specific users (e.g., your email)
            if ("admin@yourdomain.com".equals(email) || "diasnino@gmail.com".equals(email)) {
                user.addRole(Role.ADMIN);
            }

            // Initialize default notification preferences
            user.enableNotification(NotificationType.EMAIL_SECURITY);
            user.enableNotification(NotificationType.PUSH_GENERAL);
            user.enableNotification(NotificationType.IN_APP_GENERAL);

            // Record first login
            user.recordLogin();
        }

        setProviderId(user, authProvider, providerId);
        return userRepository.save(user);
    }

    // Existing methods...

    // New methods for extended user profile functionality

    /**
     * Record that a user has performed some activity
     */
    @Transactional
    public void recordUserActivity(User user) {
        user.recordActivity();
        userRepository.save(user);
    }

    /**
     * Save a user entity to the database
     */
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Get currently authenticated user
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        String email = securityContext.getAuthentication().getName();
        return findUserByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // Add original/existing methods from your UserService here...

    private AuthProvider determineAuthProvider(String providerName) {
        // Convert the provider name/URL to a supported enum value
        if (providerName.contains("google")) {
            return AuthProvider.GOOGLE;
        } else if (providerName.contains("spotify")) {
            return AuthProvider.SPOTIFY;
        } else if (providerName.contains("apple")) {
            return AuthProvider.APPLE;
        } else if (providerName.contains("soundcloud")) {
            return AuthProvider.SOUNDCLOUD;
        }
        // Default to GOOGLE if unknown
        return AuthProvider.GOOGLE;
    }

    private void setProviderId(User user, AuthProvider provider, String providerId) {
        switch (provider) {
            case GOOGLE -> user.setGoogleId(providerId);
//            case SPOTIFY -> user.setSpotifyId(providerId);
//            case APPLE -> user.setAppleId(providerId);
//            case SOUNDCLOUD -> user.setSoundcloudId(providerId);
        }
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    // Role management methods
    @Transactional
    public User assignRole(String email, Role role) {
        User user = findUserByEmail(email);
        user.addRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User removeRole(String email, Role role) {
        User user = findUserByEmail(email);
        user.getRoles().remove(role);
        return userRepository.save(user);
    }
}