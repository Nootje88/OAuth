package com.template.OAuth.config;

import com.template.OAuth.entities.RefreshToken;
import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.service.AuditService;
import com.template.OAuth.service.MessageService;
import com.template.OAuth.service.RefreshTokenService;
import com.template.OAuth.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Locale;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.application.baseUrl:http://localhost:3000}")
    private String frontendUrl;

    @Value("${LOGIN_SUCCESS_REDIRECT_URL:/home}")
    private String loginSuccessRedirectUrl;

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AppProperties appProperties;
    private final AuditService auditService;
    private final MessageService messageService;
    private final LocaleResolver localeResolver;

    public OAuth2SuccessHandler(UserService userService,
                                JwtTokenProvider jwtTokenProvider,
                                RefreshTokenService refreshTokenService,
                                AppProperties appProperties,
                                AuditService auditService,
                                MessageService messageService,
                                LocaleResolver localeResolver) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.appProperties = appProperties;
        this.auditService = auditService;
        this.messageService = messageService;
        this.localeResolver = localeResolver;

        // Properly format the default target URL
        // If frontendUrl already contains http/https and loginSuccessRedirectUrl starts with /,
        // we need to ensure we don't end up with double slashes
        String targetUrl;
        if (frontendUrl.endsWith("/") && loginSuccessRedirectUrl.startsWith("/")) {
            // Remove trailing slash from frontendUrl to prevent double slash
            targetUrl = frontendUrl.substring(0, frontendUrl.length() - 1) + loginSuccessRedirectUrl;
        } else if (!frontendUrl.endsWith("/") && !loginSuccessRedirectUrl.startsWith("/")) {
            // Add slash between parts
            targetUrl = frontendUrl + "/" + loginSuccessRedirectUrl;
        } else {
            // They're already compatible (one has slash, the other doesn't)
            targetUrl = frontendUrl + loginSuccessRedirectUrl;
        }

        // Set the default target URL - this must start with http(s) or /
        setDefaultTargetUrl(targetUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

            try {
                // Save or update user in database - this now includes login tracking
                User user = userService.saveUser(oidcUser, oidcUser.getIssuer().toString(), oidcUser.getSubject());

                // Set the user's preferred language as the locale if it exists
                if (user.getLanguagePreference() != null && !user.getLanguagePreference().isEmpty()) {
                    Locale locale = Locale.forLanguageTag(user.getLanguagePreference());
                    localeResolver.setLocale(request, response, locale);
                }

                // Generate JWT token
                String token = jwtTokenProvider.generateToken(user.getEmail());

                // Generate refresh token
                RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);

                // Set JWT in HTTP-only cookie
                Cookie cookie = new Cookie("jwt", token);
                cookie.setHttpOnly(true);
                cookie.setSecure(appProperties.getSecurity().getCookie().isSecure());
                cookie.setPath("/");
                cookie.setMaxAge((int)(appProperties.getSecurity().getJwt().getExpiration() / 1000));
                response.addCookie(cookie);

                // Set refresh token in a different cookie
                Cookie refreshCookie = new Cookie("refresh_token", refreshToken.getToken());
                refreshCookie.setHttpOnly(true);
                refreshCookie.setSecure(appProperties.getSecurity().getCookie().isSecure());
                refreshCookie.setPath("/refresh-token");
                refreshCookie.setMaxAge((int)(appProperties.getSecurity().getRefresh().getExpiration() / 1000));
                response.addCookie(refreshCookie);

                // Audit successful OAuth login
                boolean isNewUser = user.getLoginCount() == 1;
                if (isNewUser) {
                    auditService.logEvent(AuditEventType.USER_CREATED,
                            messageService.getMessage("user.created"),
                            "User: " + user.getEmail() + ", Provider: " + oidcUser.getIssuer().toString());
                }

                auditService.logEvent(AuditEventType.LOGIN_SUCCESS,
                        messageService.getMessage("auth.login.success"),
                        "User: " + user.getEmail() + ", Provider: " + oidcUser.getIssuer().toString());

                // Redirect to frontend home page
                getRedirectStrategy().sendRedirect(request, response, getDefaultTargetUrl());
            } catch (Exception e) {
                logger.error("Error in OAuth authentication success handler", e);

                // Audit login failure
                auditService.logEvent(AuditEventType.LOGIN_FAILURE,
                        messageService.getMessage("auth.login.failure"),
                        "Error: " + e.getMessage());

                // Format the error redirect URL
                String errorRedirectUrl;
                if (frontendUrl.endsWith("/")) {
                    errorRedirectUrl = frontendUrl + "login?error=auth";
                } else {
                    errorRedirectUrl = frontendUrl + "/login?error=auth";
                }

                response.sendRedirect(errorRedirectUrl);
            }
        } else {
            logger.error("Authentication principal is not OidcUser");

            // Audit login failure
            auditService.logEvent(AuditEventType.LOGIN_FAILURE,
                    messageService.getMessage("auth.login.failure"),
                    "Error: Authentication principal is not OidcUser");

            // Format the error redirect URL
            String errorRedirectUrl;
            if (frontendUrl.endsWith("/")) {
                errorRedirectUrl = frontendUrl + "login?error=type";
            } else {
                errorRedirectUrl = frontendUrl + "/login?error=type";
            }

            response.sendRedirect(errorRedirectUrl);
        }
    }
}