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
import org.springframework.stereotype.Service;
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

    public RefreshToken generateRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenProvider.generateRefreshToken());
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60)); // 7 days
        return refreshTokenRepository.save(refreshToken);
    }

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
            refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
            refreshTokenRepository.save(refreshToken);

            // Store in HTTP-only cookie
            Cookie cookie = new Cookie("jwt", newAccessToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(3600); // Expires in 1 hour
            response.addCookie(cookie);

            return new RefreshTokenResponse(newAccessToken, newRefreshToken, "Token refreshed successfully");
        } else {
            throw new RuntimeException("Invalid refresh token");
        }
    }
}
