package com.template.OAuth.controller;

import com.template.OAuth.annotation.Auditable;
import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.dto.AuthResponse;
import com.template.OAuth.dto.UserLoginRequest;
import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.repositories.RefreshTokenRepository;
import com.template.OAuth.repositories.UserRepository;
import com.template.OAuth.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          AuditService auditService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditService = auditService;
    }

    @Operation(summary = "Authenticate user",
            description = "Authenticates a user with provided credentials from OAuth providers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials",
                    content = @Content)
    })
    @SecurityRequirements // No security requirements for login endpoint
    @PostMapping("/login")
    @Auditable(type = AuditEventType.LOGIN_SUCCESS, description = "User login")
    public ResponseEntity<AuthResponse> authenticateUser(
            @Parameter(description = "User login details", required = true)
            @RequestBody UserLoginRequest loginRequest,
            HttpServletResponse response) {

        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        User user;
        boolean isNewUser = false;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            user = new User();
            user.setEmail(loginRequest.getEmail());
            user.setName(loginRequest.getName());
            user.setPicture(loginRequest.getPicture());
            user.setPrimaryProvider(AuthProvider.GOOGLE);
            isNewUser = true;
            userRepository.save(user);

            // Audit new user creation
            auditService.logEvent(AuditEventType.USER_CREATED,
                    "New user registered",
                    "Email: " + user.getEmail());
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());

        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour expiration
        response.addCookie(cookie);

        // Audit successful login
        auditService.logEvent(AuditEventType.LOGIN_SUCCESS,
                "User logged in successfully",
                "Email: " + user.getEmail());

        return ResponseEntity.ok(new AuthResponse(token, "Login successful"));
    }

    @Operation(summary = "Get current user",
            description = "Retrieves the current authenticated user from the JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content)
    })
    @GetMapping("/user")
    public ResponseEntity<?> getUserFromToken(HttpServletRequest request) {
        Optional<String> tokenOpt = jwtTokenProvider.getTokenFromRequest(request);

        if (tokenOpt.isPresent()) {
            String token = tokenOpt.get();
            if (jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                Optional<User> user = userRepository.findByEmail(email);

                if (user.isPresent()) {
                    return ResponseEntity.ok(user.get());
                }
            }
        }

        return ResponseEntity.status(401).body("Unauthorized");
    }

    @Operation(summary = "Logout",
            description = "Logs out the current user by clearing auth cookies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content)
    })
    @PostMapping("/logout")
    @Auditable(type = AuditEventType.LOGOUT, description = "User logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        // Get current user email from the token for audit logging
        String userEmail = "anonymous";
        Optional<String> tokenOpt = jwtTokenProvider.getTokenFromRequest(request);
        if (tokenOpt.isPresent()) {
            try {
                userEmail = jwtTokenProvider.getEmailFromToken(tokenOpt.get());
            } catch (Exception e) {
                // Token might be invalid, just continue with logout
            }
        }

        // Clear JWT cookie
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Set to true in production
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        // Clear refresh token cookie
        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Set to true in production
        refreshCookie.setPath("/refresh-token");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        // Clean up refresh tokens for this user
        if (!"anonymous".equals(userEmail)) {
            userRepository.findByEmail(userEmail).ifPresent(user ->
                    refreshTokenRepository.deleteByUser(user));

            // Audit logout event
            auditService.logEvent(AuditEventType.LOGOUT,
                    "User logged out successfully",
                    "Email: " + userEmail);
        }

        return ResponseEntity.ok("Logged out successfully");
    }

    @Operation(summary = "Get login URL",
            description = "Returns the URL for OAuth2 authentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login URL retrieved successfully")
    })
    @SecurityRequirements // No security requirements for login URL endpoint
    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl() {
        return ResponseEntity.ok("/oauth2/authorization/google");
    }
}