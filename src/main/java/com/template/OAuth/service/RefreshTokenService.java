package com.template.OAuth.service;

import com.template.OAuth.config.AppProperties;
import com.template.OAuth.config.JwtTokenProvider;
import com.template.OAuth.config.RefreshTokenProvider;
import com.template.OAuth.dto.RefreshTokenResponse;
import com.template.OAuth.entities.RefreshToken;
import com.template.OAuth.entities.User;
import com.template.OAuth.repositories.RefreshTokenRepository;
import com.template.OAuth.repositories.UserRepository;
import com.template.OAuth.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AppProperties appProperties;

    @Transactional
    public RefreshToken generateRefreshToken(User user) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        if (existingToken.isPresent()) {
            RefreshToken rt = existingToken.get();
            rt.setToken(refreshTokenProvider.generateRefreshToken());
            rt.setExpiryDate(Instant.now().plusMillis(appProperties.getSecurity().getRefresh().getExpiration()));
            return refreshTokenRepository.save(rt);
        } else {
            RefreshToken rt = new RefreshToken();
            rt.setUser(user);
            rt.setToken(refreshTokenProvider.generateRefreshToken());
            rt.setExpiryDate(Instant.now().plusMillis(appProperties.getSecurity().getRefresh().getExpiration()));
            return refreshTokenRepository.save(rt);
        }
    }

    @Transactional
    public RefreshTokenResponse refreshToken(String oldRefreshToken, HttpServletResponse response) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(oldRefreshToken);
        if (refreshTokenOpt.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token has expired, please log in again.");
        }

        // Generate new JWT
        String newAccessToken = jwtTokenProvider.generateToken(refreshToken.getUser().getEmail());

        // Rotate refresh token
        String newRefreshToken = refreshTokenProvider.generateRefreshToken();
        refreshToken.setToken(newRefreshToken);
        refreshToken.setExpiryDate(Instant.now().plusMillis(appProperties.getSecurity().getRefresh().getExpiration()));
        refreshTokenRepository.save(refreshToken);

        // Set cookies (HttpOnly/SameSite/Secure handled centrally)
        long accessTtlSeconds  = appProperties.getSecurity().getJwt().getExpiration() / 1000;
        long refreshTtlSeconds = appProperties.getSecurity().getRefresh().getExpiration() / 1000;

        CookieUtil.addCookie(response, "jwt", newAccessToken, "/", accessTtlSeconds, appProperties);
        CookieUtil.addCookie(response, "refresh_token", newRefreshToken, "/refresh-token", refreshTtlSeconds, appProperties);

        return new RefreshTokenResponse(newAccessToken, newRefreshToken, "Token refreshed successfully");
    }

    @Transactional
    public void revokeAllForUser(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Either is fine; keep both available in the repo
            refreshTokenRepository.deleteAllByUser(user);
        });
    }
}
