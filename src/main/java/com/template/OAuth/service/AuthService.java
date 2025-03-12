package com.template.OAuth.service;

import com.template.OAuth.config.AppProperties;
import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.dto.EmailLoginRequest;
import com.template.OAuth.dto.EmailRegistrationRequest;
import com.template.OAuth.entities.RefreshToken;
import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.enums.NotificationType;
import com.template.OAuth.enums.Role;
import com.template.OAuth.repositories.UserRepository;
import com.template.OAuth.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final AuthenticationManager authenticationManager;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       EmailService emailService,
                       RefreshTokenService refreshTokenService,
                       AuditService auditService,
                       AuthenticationManager authenticationManager,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.authenticationManager = authenticationManager;
        this.appProperties = appProperties;
    }

    @Transactional
    public User registerUser(EmailRegistrationRequest registrationRequest) {
        // Check if user already exists
        if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already in use");
        }

        // Create new user
        User user = new User();
        user.setName(registrationRequest.getName());
        user.setEmail(registrationRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setPrimaryProvider(AuthProvider.LOCAL);
        user.setEnabled(false); // Not enabled until email verification

        // Generate verification token
        String token = generateToken();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(Instant.now().plusSeconds(
                appProperties.getSecurity().getVerification().getExpirationHours() * 3600L));

        // Assign default USER role
        user.addRole(Role.USER);

        // Set default notification preferences
        user.enableNotification(NotificationType.EMAIL_SECURITY);
        user.enableNotification(NotificationType.IN_APP_GENERAL);

        // Save user
        user = userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);

        // Log the registration event
        auditService.logEvent(
                AuditEventType.USER_CREATED,
                "New user registered with email authentication",
                "User: " + user.getEmail()
        );

        return user;
    }

    @Transactional
    public boolean verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        // Check if token is expired
        if (user.getVerificationTokenExpiry().isBefore(Instant.now())) {
            throw new RuntimeException("Verification token has expired");
        }

        // Enable user account
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        // Record first login
        user.recordLogin();

        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());

        // Log the event
        auditService.logEvent(
                AuditEventType.USER_UPDATED,
                "User email verified",
                "User: " + user.getEmail()
        );

        return true;
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            throw new RuntimeException("User is already verified");
        }

        // Generate new verification token
        String token = generateToken();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(Instant.now().plusSeconds(
                appProperties.getSecurity().getVerification().getExpirationHours() * 3600L));

        userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);

        // Log the event
        auditService.logEvent(
                AuditEventType.SYSTEM_EVENT,
                "Verification email resent",
                "User: " + user.getEmail()
        );
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Even if user doesn't exist, don't reveal that to potential attackers
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Generate password reset token
            String token = generateToken();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(Instant.now().plusSeconds(
                    appProperties.getSecurity().getPasswordReset().getExpirationHours() * 3600L));

            userRepository.save(user);

            // Send password reset email
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);

            // Log the event
            auditService.logEvent(
                    AuditEventType.SYSTEM_EVENT,
                    "Password reset initiated",
                    "User: " + user.getEmail()
            );
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

        // Check if token is expired
        if (user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new RuntimeException("Password reset token has expired");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);

        // Log the event
        auditService.logEvent(
                AuditEventType.USER_UPDATED,
                "User password reset",
                "User: " + user.getEmail()
        );

        return true;
    }

    @Transactional
    public void authenticateAndGenerateTokens(EmailLoginRequest loginRequest, HttpServletResponse response) {
        try {
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Get user from authentication
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByEmail(userPrincipal.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Record login
            user.recordLogin();
            userRepository.save(user);

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(user.getEmail());

            // Generate refresh token
            RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);

            // Store in HTTP-only cookies
            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(appProperties.getSecurity().getCookie().isSecure());
            cookie.setPath("/");
            cookie.setMaxAge((int) (appProperties.getSecurity().getJwt().getExpiration() / 1000));
            response.addCookie(cookie);

            // Set refresh token in a different cookie
            Cookie refreshCookie = new Cookie("refresh_token", refreshToken.getToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(appProperties.getSecurity().getCookie().isSecure());
            refreshCookie.setPath("/refresh-token");
            refreshCookie.setMaxAge((int) (appProperties.getSecurity().getRefresh().getExpiration() / 1000));
            response.addCookie(refreshCookie);

            // Log successful login
            auditService.logEvent(
                    AuditEventType.LOGIN_SUCCESS,
                    "User logged in with email and password",
                    "User: " + user.getEmail()
            );

        } catch (DisabledException e) {
            auditService.logEvent(
                    AuditEventType.LOGIN_FAILURE,
                    "Login attempt with disabled account",
                    "Email: " + loginRequest.getEmail()
            );
            throw new RuntimeException("Account is disabled, please verify your email");
        } catch (BadCredentialsException e) {
            auditService.logEvent(
                    AuditEventType.LOGIN_FAILURE,
                    "Login attempt with invalid credentials",
                    "Email: " + loginRequest.getEmail()
            );
            throw new RuntimeException("Invalid email or password");
        }
    }

    // Utility method to generate a secure random token
    private String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}