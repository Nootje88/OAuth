package com.template.OAuth.config;

import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.service.AuditService;
import com.template.OAuth.service.MessageService;
import com.template.OAuth.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2FailureHandler.class);
    private final AppProperties appProperties;
    private final AuditService auditService;
    private final MessageService messageService;

    public OAuth2FailureHandler(AppProperties appProperties,
                                AuditService auditService,
                                MessageService messageService) {
        this.appProperties = appProperties;
        this.auditService = auditService;
        this.messageService = messageService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {

        logger.warn("OAuth2 login failed: {}", exception.getMessage());

        // Clear any existing cookies
        CookieUtil.clearCookie(response, "jwt", appProperties);
        CookieUtil.clearCookie(response, "refresh_token", appProperties);

        // Log audit event
        auditService.logEvent(AuditEventType.LOGIN_FAILURE,
                messageService.getMessage("auth.login.failure"),
                "OAuth2 failure: " + exception.getMessage());

        // Redirect back to frontend login page with error query param
        String redirectUrl = appProperties.getApplication().getBaseUrl() + "/login?error=oauth2";
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
