package com.template.OAuth.controller;

import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.dto.AuthResponse;
import com.template.OAuth.entities.User;
import com.template.OAuth.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public AuthController(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestParam String email, HttpServletResponse response) {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            // Generate JWT Token
            String token = jwtTokenProvider.generateToken(email);

            // Store in HTTP-only cookie
            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);  // Prevent JavaScript access
            cookie.setSecure(true);  // Only send over HTTPS
            cookie.setPath("/");  // Available across all endpoints
            cookie.setMaxAge(3600);  // Expires in 1 hour
            response.addCookie(cookie);

            return ResponseEntity.ok(new AuthResponse(null, "Login successful")); // Token is no longer in response body
        } else {
            return ResponseEntity.badRequest().body(new AuthResponse(null, "User not found"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // Remove the cookie by setting maxAge=0
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }
}
