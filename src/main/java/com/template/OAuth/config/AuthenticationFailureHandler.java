package com.template.OAuth.config;

import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.service.AuditService;
import com.template.OAuth.service.MessageService;
import com.template.OAuth.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AppProperties appProperties;
    private final AuditService auditService;
    private final MessageService messageService;

    public AuthenticationFailureHandler(AppProperties appProperties,
                                        AuditService auditService,
                                        MessageService messageService) {
        this.appProperties = appProperties;
        this.auditService = auditService;
        this.messageService = messageService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        // Clear cookies to remove old tokens
        CookieUtil.clearCookie(response, "jwt", appProperties);
        CookieUtil.clearCookie(response, "refresh_token", appProperties);

        auditService.logEvent(
                AuditEventType.LOGIN_FAILURE,
                messageService.getMessage("auth.login.failure"),
                "Failure reason: " + exception.getMessage()
        );

        // Respond with 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Invalid credentials");
    }
}
