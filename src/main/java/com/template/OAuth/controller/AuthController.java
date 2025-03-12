package com.template.OAuth.controller;

import com.template.OAuth.annotation.Auditable;
import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.dto.*;
import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.repositories.RefreshTokenRepository;
import com.template.OAuth.repositories.UserRepository;
import com.template.OAuth.service.AuditService;
import com.template.OAuth.service.AuthService;
import com.template.OAuth.service.MessageService;
import com.template.OAuth.service.MetricsService;
import io.micrometer.core.annotation.Timed;
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
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
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
    private final MetricsService metricsService;
    private final MessageService messageService;
    private final AuthService authService;

    @Autowired
    public AuthController(JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          AuditService auditService,
                          MetricsService metricsService,
                          MessageService messageService,
                          AuthService authService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.messageService = messageService;
        this.authService = authService;
    }

    @Operation(summary = "Register a new user with email and password",
            description = "Registers a new user account that requires email verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or email already in use",
                    content = @Content)
    })
    @SecurityRequirements // No security requirements for registration endpoint
    @PostMapping("/register")
    @Timed(value = "auth.register.time", description = "Time taken to register new user")
    public ResponseEntity<Map<String, String>> registerUser(
            @Parameter(description = "User registration details", required = true)
            @Valid @RequestBody EmailRegistrationRequest registrationRequest) {

        try {
            User user = authService.registerUser(registrationRequest);

            // Record metrics for new user registration
            metricsService.incrementRegistration();

            Map<String, String> response = new HashMap<>();
            response.put("message", messageService.getMessage("user.created"));
            response.put("email", user.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Verify email address",
            description = "Verifies a user's email address using the token sent in the verification email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token",
                    content = @Content)
    })
    @SecurityRequirements // No security requirements for verification endpoint
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Parameter(description = "Verification token", required = true)
            @RequestParam String token) {

        try {
            boolean verified = authService.verifyEmail(token);

            Map<String, String> response = new HashMap<>();
            if (verified) {
                response.put("message", messageService.getMessage("email.verified"));
                return ResponseEntity.ok(response);
            } else {
                response.put("message", messageService.getMessage("email.verification.failed"));
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Resend verification email",
            description = "Resends the verification email for unverified accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email or account already verified",
                    content = @Content)
    })
    @SecurityRequirements // No security requirements for resend verification
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(
            @Parameter(description = "Email address", required = true)
            @RequestParam String email) {

        try {
            authService.resendVerificationEmail(email);

            Map<String, String> response = new HashMap<>();
            response.put("message", messageService.getMessage("email.verification.resent"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Initiate password reset",
            description = "Sends a password reset email to the specified email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent successfully")
    })
    @SecurityRequirements // No security requirements for password reset
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Parameter(description = "Password reset request", required = true)
            @Valid @RequestBody PasswordResetRequest resetRequest) {

        authService.initiatePasswordReset(resetRequest.getEmail());

        // Always return success, even if email doesn't exist (security measure)
        Map<String, String> response = new HashMap<>();
        response.put("message", messageService.getMessage("email.password.reset.sent"));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset password",
            description = "Resets the user's password using the token sent in the reset email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token",
                    content = @Content)
    })
    @SecurityRequirements // No security requirements for password reset
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Parameter(description = "Password reset completion data", required = true)
            @Valid @RequestBody PasswordResetCompletion resetCompletion) {

        try {
            boolean reset = authService.resetPassword(resetCompletion.getToken(), resetCompletion.getPassword());

            Map<String, String> response = new HashMap<>();
            if (reset) {
                response.put("message", messageService.getMessage("password.reset.success"));
                return ResponseEntity.ok(response);
            } else {
                response.put("message", messageService.getMessage("password.reset.failed"));
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Login with email and password",
            description = "Authenticates a user with email and password credentials")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or unverified account",
                    content = @Content)
    })
    @SecurityRequirements // No security requirements for login endpoint
    @PostMapping("/email-login")
    @Timed(value = "auth.email.login.time", description = "Time taken to perform email login")
    public ResponseEntity<AuthResponse> loginWithEmail(
            @Parameter(description = "User login details", required = true)
            @Valid @RequestBody EmailLoginRequest loginRequest,
            HttpServletResponse response) {

        try {
            authService.authenticateAndGenerateTokens(loginRequest, response);

            // Record metrics for successful login
            metricsService.incrementAuthSuccess();

            return ResponseEntity.ok(new AuthResponse(null, messageService.getMessage("auth.login.success")));
        } catch (Exception e) {
            // Record metrics for failed login
            metricsService.incrementAuthFailure();

            return ResponseEntity.badRequest().body(new AuthResponse(null, e.getMessage()));
        }
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
    @Timed(value = "auth.login.time", description = "Time taken to perform login")
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
            user.setPrimaryProvider(com.template.OAuth.enums.AuthProvider.GOOGLE);
            user.setEnabled(true); // OAuth users are automatically verified
            isNewUser = true;
            userRepository.save(user);

            // Record metrics for new user registration
            metricsService.incrementRegistration();

            // Audit new user creation
            auditService.logEvent(AuditEventType.USER_CREATED,
                    messageService.getMessage("user.created"),
                    "Email: " + user.getEmail());
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());

        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour expiration
        response.addCookie(cookie);

        // Record metrics for successful login
        metricsService.incrementAuthSuccess();

        // Audit successful login
        auditService.logEvent(AuditEventType.LOGIN_SUCCESS,
                messageService.getMessage("auth.login.success"),
                "Email: " + user.getEmail());

        return ResponseEntity.ok(new AuthResponse(token, messageService.getMessage("auth.login.success")));
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
    @Timed(value = "auth.get-user.time", description = "Time taken to get current user")
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

        // Record metrics for unauthorized access
        metricsService.incrementError("unauthorized");

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", messageService.getMessage("auth.access.denied"));
        return ResponseEntity.status(401).body(errorResponse);
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
    @Timed(value = "auth.logout.time", description = "Time taken to logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
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

            // Record metrics for logout
            metricsService.incrementLogout();

            // Audit logout event
            auditService.logEvent(AuditEventType.LOGOUT,
                    messageService.getMessage("auth.logout.success"),
                    "Email: " + userEmail);
        }

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("message", messageService.getMessage("auth.logout.success"));
        return ResponseEntity.ok(responseMap);
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