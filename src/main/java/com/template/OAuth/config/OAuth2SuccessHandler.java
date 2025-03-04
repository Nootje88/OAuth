package com.template.OAuth.config;

import com.template.OAuth.entities.RefreshToken;
import com.template.OAuth.entities.User;
import com.template.OAuth.service.RefreshTokenService;
import com.template.OAuth.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public OAuth2SuccessHandler(UserService userService, JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        setDefaultTargetUrl("http://localhost:3000/home");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

            try {
                // Save or update user in database
                User user = userService.saveUser(oidcUser, oidcUser.getIssuer().toString(), oidcUser.getSubject());

                // Generate JWT token
                String token = jwtTokenProvider.generateToken(user.getEmail());

                // Generate refresh token
                RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);

                // Set JWT in HTTP-only cookie
                Cookie cookie = new Cookie("jwt", token);
                cookie.setHttpOnly(true);
                cookie.setSecure(false); // Set to true in production
                cookie.setPath("/");
                cookie.setMaxAge(3600); // 1 hour
                response.addCookie(cookie);

                // Set refresh token in a different cookie
                Cookie refreshCookie = new Cookie("refresh_token", refreshToken.getToken());
                refreshCookie.setHttpOnly(true);
                refreshCookie.setSecure(false); // Set to true in production
                refreshCookie.setPath("/refresh-token");
                refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
                response.addCookie(refreshCookie);

                // Redirect to frontend home page
                getRedirectStrategy().sendRedirect(request, response, getDefaultTargetUrl());
            } catch (Exception e) {
                logger.error("Error in OAuth authentication success handler", e);
                response.sendRedirect("http://localhost:3000/login?error=auth");
            }
        } else {
            logger.error("Authentication principal is not OidcUser");
            response.sendRedirect("http://localhost:3000/login?error=type");
        }
    }
}