package com.template.OAuth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.OAuth.dto.EmailLoginRequest;
import com.template.OAuth.dto.EmailRegistrationRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private EmailRegistrationRequest registrationRequest;
    private EmailLoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Delete refresh tokens first (they reference users)
        refreshTokenRepository.deleteAll();

        // Clean up the user repository before each test
        userRepository.deleteAll();

        // Set up registration request
        registrationRequest = new EmailRegistrationRequest();
        registrationRequest.setEmail("integration@example.com");
        registrationRequest.setName("Integration Test");
        registrationRequest.setPassword("Password123!");

        // Set up login request
        loginRequest = new EmailLoginRequest();
        loginRequest.setEmail("integration@example.com");
        loginRequest.setPassword("Password123!");
    }

    @Test
    void testRegistrationAndLogin() throws Exception {
        // Step 1: Register the user
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.email").value("integration@example.com"));

        // Extract verification token from backend logic in a real scenario
        // For this integration test, we'd manually set the user to verified in DB
        // Since we can't control email verification in tests

        // Verify user exists in DB but is not enabled yet
        var users = userRepository.findByEmail("integration@example.com");
        assert users.isPresent();
        var user = users.get();
        assert !user.isEnabled();

        // Manually enable the user for testing purposes
        user.setEnabled(true);
        userRepository.save(user);

        // Step 2: Login with the registered user
        MvcResult loginResult = mockMvc.perform(post("/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        // Verify cookies are set
        var cookies = loginResult.getResponse().getCookies();
        assert cookies.length >= 2;
    }

    @Test
    void testInvalidLogin() throws Exception {
        // Create wrong login request
        EmailLoginRequest wrongLoginRequest = new EmailLoginRequest();
        wrongLoginRequest.setEmail("wrong@example.com");
        wrongLoginRequest.setPassword("WrongPassword123!");

        // Attempt to login with invalid credentials
        mockMvc.perform(post("/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLoginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
