package com.template.OAuth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.OAuth.dto.EmailRegistrationRequest;
import com.template.OAuth.entities.User;
import com.template.OAuth.repositories.RefreshTokenRepository;
import com.template.OAuth.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailVerificationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private EmailRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        // Delete refresh tokens first (they reference users)
        refreshTokenRepository.deleteAll();

        // Clean up user repository
        userRepository.deleteAll();

        // Set up registration request
        registrationRequest = new EmailRegistrationRequest();
        registrationRequest.setEmail("verificationtest@example.com");
        registrationRequest.setName("Verification Test");
        registrationRequest.setPassword("Password123!");
    }

    @Test
    void testEmailVerificationFlow() throws Exception {
        // Step 1: Register the user
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("verificationtest@example.com"));

        // Step 2: Verify the user is created but not enabled
        Optional<User> userOpt = userRepository.findByEmail("verificationtest@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertFalse(user.isEnabled(), "User should not be enabled before verification");
        assertNotNull(user.getVerificationToken(), "Verification token should be set");

        // Step 3: Extract verification token and verify email
        String verificationToken = user.getVerificationToken();
        mockMvc.perform(get("/auth/verify-email")
                        .param("token", verificationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Step 4: Check user is now enabled
        userOpt = userRepository.findByEmail("verificationtest@example.com");
        assertTrue(userOpt.isPresent());
        user = userOpt.get();
        assertTrue(user.isEnabled(), "User should be enabled after verification");
        assertNull(user.getVerificationToken(), "Verification token should be cleared");

        // Step 5: Try to login with the verified account
        mockMvc.perform(post("/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"verificationtest@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidVerificationToken() throws Exception {
        // Register a user first
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk());

        // Try to verify with an invalid token
        mockMvc.perform(get("/auth/verify-email")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest());

        // Verify user is still not enabled
        Optional<User> userOpt = userRepository.findByEmail("verificationtest@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertFalse(user.isEnabled(), "User should still be disabled after invalid verification");
    }

    @Test
    void testResendVerificationEmail() throws Exception {
        // Register a user first
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk());

        // Get the initial verification token
        Optional<User> userOpt = userRepository.findByEmail("verificationtest@example.com");
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        String initialToken = user.getVerificationToken();

        // Request a new verification email
        mockMvc.perform(post("/auth/resend-verification")
                        .param("email", "verificationtest@example.com"))
                .andExpect(status().isOk());

        // Verify the token has changed
        userOpt = userRepository.findByEmail("verificationtest@example.com");
        assertTrue(userOpt.isPresent());
        user = userOpt.get();
        String newToken = user.getVerificationToken();

        assertNotEquals(initialToken, newToken, "Verification token should be changed after resend");
    }
}