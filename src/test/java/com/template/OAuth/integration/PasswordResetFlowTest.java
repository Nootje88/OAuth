package com.template.OAuth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.OAuth.dto.EmailRegistrationRequest;
import com.template.OAuth.dto.PasswordResetCompletion;
import com.template.OAuth.dto.PasswordResetRequest;
import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.repositories.RefreshTokenRepository;
import com.template.OAuth.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Delete refresh tokens first (they reference users)
        refreshTokenRepository.deleteAll();

        // Clean up user repository
        userRepository.deleteAll();

        // Create a test user
        testUser = new User();
        testUser.setEmail("passwordreset@example.com");
        testUser.setName("Password Reset Test");
        testUser.setPassword(passwordEncoder.encode("OldPassword123!"));
        testUser.setEnabled(true);
        testUser.setPrimaryProvider(AuthProvider.LOCAL);

        userRepository.save(testUser);
    }

    @Test
    void testPasswordResetFlow() throws Exception {
        // Step 1: Request password reset
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setEmail("passwordreset@example.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Step 2: Verify reset token was generated
        Optional<User> userOpt = userRepository.findByEmail("passwordreset@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertNotNull(user.getPasswordResetToken(), "Password reset token should be set");
        assertNotNull(user.getPasswordResetTokenExpiry(), "Password reset token expiry should be set");

        String resetToken = user.getPasswordResetToken();

        // Step 3: Complete password reset
        PasswordResetCompletion resetCompletion = new PasswordResetCompletion();
        resetCompletion.setToken(resetToken);
        resetCompletion.setPassword("NewPassword456!");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetCompletion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Step 4: Verify user's password has changed
        userOpt = userRepository.findByEmail("passwordreset@example.com");
        assertTrue(userOpt.isPresent());
        user = userOpt.get();
        assertNull(user.getPasswordResetToken(), "Password reset token should be cleared");
        assertNull(user.getPasswordResetTokenExpiry(), "Password reset token expiry should be cleared");

        assertTrue(passwordEncoder.matches("NewPassword456!", user.getPassword()),
                "Password should be updated to the new value");
        assertFalse(passwordEncoder.matches("OldPassword123!", user.getPassword()),
                "Old password should no longer work");

        // Step 5: Try to login with the new password
        mockMvc.perform(post("/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"passwordreset@example.com\",\"password\":\"NewPassword456!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidResetToken() throws Exception {
        PasswordResetCompletion resetCompletion = new PasswordResetCompletion();
        resetCompletion.setToken("invalid-token");
        resetCompletion.setPassword("NewPassword456!");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetCompletion)))
                .andExpect(status().isBadRequest());

        // Verify password wasn't changed
        Optional<User> userOpt = userRepository.findByEmail("passwordreset@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertTrue(passwordEncoder.matches("OldPassword123!", user.getPassword()),
                "Password should not change with invalid token");
    }
}