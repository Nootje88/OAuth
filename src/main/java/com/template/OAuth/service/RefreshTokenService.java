package com.template.OAuth.service;

import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.config.RefreshTokenProvider;
import com.template.OAuth.dto.RefreshTokenResponse;
import com.template.OAuth.entities.RefreshToken;
import com.template.OAuth.entities.User;
import com.template.OAuth.repositories.RefreshTokenRepository;
import com.template.OAuth.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenProvider refreshTokenProvider;

    @Value("${app.security.jwt.expiration:3600000}")
    private long jwtExpirationMs; // Default: 1 hour

    @Value("${app.security.refresh.expiration:604800000}")
    private long refreshTokenExpirationMs; // Default: 7 days

    @Transactional
    public RefreshToken generateRefreshToken(User user) {
        // First check if the user already has a refresh token
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        if (existingToken.isPresent()) {
            // Update existing token instead of creating a new one
            RefreshToken refreshToken = existingToken.get();
            refreshToken.setToken(refreshTokenProvider.generateRefreshToken());
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
            return refreshTokenRepository.save(refreshToken);
        } else {
            // Create new token
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setToken(refreshTokenProvider.generateRefreshToken());
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
            return refreshTokenRepository.save(refreshToken);
        }
    }

    @Transactional
    public RefreshTokenResponse refreshToken(String oldRefreshToken, HttpServletResponse response) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(oldRefreshToken);

        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();

            if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                refreshTokenRepository.delete(refreshToken);
                throw new RuntimeException("Refresh token has expired, please log in again.");
            }

            // Generate new JWT
            String newAccessToken = jwtTokenProvider.generateToken(refreshToken.getUser().getEmail());

            // Issue new refresh token
            String newRefreshToken = refreshTokenProvider.generateRefreshToken();
            refreshToken.setToken(newRefreshToken);
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
            refreshTokenRepository.save(refreshToken);

            // Store in HTTP-only cookie
            Cookie cookie = new Cookie("jwt", newAccessToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Set to true in production
            cookie.setPath("/");
            cookie.setMaxAge((int)(jwtExpirationMs / 1000)); // Convert ms to seconds for cookie
            response.addCookie(cookie);

            // Set refresh token in a different cookie
            Cookie refreshCookie = new Cookie("refresh_token", newRefreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false); // Set to true in production
            refreshCookie.setPath("/refresh-token");
            refreshCookie.setMaxAge((int)(refreshTokenExpirationMs / 1000)); // Convert ms to seconds
            response.addCookie(refreshCookie);

            return new RefreshTokenResponse(newAccessToken, newRefreshToken, "Token refreshed successfully");
        } else {
            throw new RuntimeException("Invalid refresh token");
        }
    }
}