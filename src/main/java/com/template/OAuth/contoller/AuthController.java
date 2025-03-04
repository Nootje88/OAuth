package com.template.OAuth.contoller;

import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.dto.AuthResponse;
import com.template.OAuth.dto.UserLoginRequest;
import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.repositories.RefreshTokenRepository;
import com.template.OAuth.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthController(JwtTokenProvider jwtTokenProvider, UserRepository userRepository,
                          RefreshTokenRepository refreshTokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestBody UserLoginRequest loginRequest, HttpServletResponse response) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            user = new User();
            user.setEmail(loginRequest.getEmail());
            user.setName(loginRequest.getName());
            user.setPicture(loginRequest.getPicture());
            user.setPrimaryProvider(AuthProvider.GOOGLE);
            userRepository.save(user);
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());

        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour expiration
        response.addCookie(cookie);

        return ResponseEntity.ok(new AuthResponse(token, "Login successful"));
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserFromToken(HttpServletRequest request) {
        // Try to get token from cookies
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

        // Don't try to get user from OAuth context, just return 401
        return ResponseEntity.status(401).body("Unauthorized");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
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

        // Get current user email from the token
        Optional<String> tokenOpt = jwtTokenProvider.getTokenFromRequest(request);
        if (tokenOpt.isPresent()) {
            try {
                String email = jwtTokenProvider.getEmailFromToken(tokenOpt.get());
                // Clean up refresh tokens for this user
                userRepository.findByEmail(email).ifPresent(user ->
                        refreshTokenRepository.deleteByUser(user));
            } catch (Exception e) {
                // Token might be invalid, just continue with logout
            }
        }

        return ResponseEntity.ok("Logged out successfully");
    }

    // Add simple endpoint to get the login URL
    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl() {
        return ResponseEntity.ok("/oauth2/authorization/google");
    }
}