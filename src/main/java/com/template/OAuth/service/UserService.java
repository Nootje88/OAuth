package com.template.OAuth.service;

import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User saveUser(OidcUser oidcUser, String providerName, String providerId) {
        String email = oidcUser.getEmail();
        Optional<User> existingUser = userRepository.findByEmail(email);
        AuthProvider authProvider = AuthProvider.valueOf(providerName.toUpperCase());

        User user = existingUser.orElseGet(() -> {
            User newUser = new User();
            newUser.setName(oidcUser.getFullName());
            newUser.setEmail(email);
            newUser.setPicture(oidcUser.getPicture());
            newUser.setPrimaryProvider(authProvider);
            return newUser;
        });

        setProviderId(user, authProvider, providerId);
        return userRepository.save(user);
    }

    private void setProviderId(User user, AuthProvider provider, String providerId) {
        switch (provider) {
            case GOOGLE -> user.setGoogleId(providerId);
            case SPOTIFY -> user.setSpotifyId(providerId);
            case APPLE -> user.setAppleId(providerId);
            case SOUNDCLOUD -> user.setSoundcloudId(providerId);
        }
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
