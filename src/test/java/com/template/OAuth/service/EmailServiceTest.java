package com.template.OAuth.service;

import com.template.OAuth.config.AppProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.Email email;

    @Mock
    private AppProperties.Application application;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mocks
        when(appProperties.getEmail()).thenReturn(email);
        when(appProperties.getApplication()).thenReturn(application);

        when(email.getFromAddress()).thenReturn("noreply@example.com");
        when(email.getFromName()).thenReturn("Test App");

        when(application.getBaseUrl()).thenReturn("http://localhost:3000");
        when(application.getSupportEmail()).thenReturn("support@example.com");

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html><body>Test Email Content</body></html>");

        when(messageService.getMessage("email.verification.subject")).thenReturn("Verify Your Email");
        when(messageService.getMessage("email.password.reset.subject")).thenReturn("Reset Your Password");
        when(messageService.getMessage("email.welcome.subject")).thenReturn("Welcome to Our App");
    }

    @Test
    void testSendVerificationEmail() {
        // Arrange
        String email = "test@example.com";
        String name = "Test User";
        String token = "verification-token";

        // Act
        emailService.sendVerificationEmail(email, name, token);

        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine, times(1)).process(eq("email-verification"), any(IContext.class));
    }

    @Test
    void testSendPasswordResetEmail() {
        // Arrange
        String email = "test@example.com";
        String name = "Test User";
        String token = "reset-token";

        // Act
        emailService.sendPasswordResetEmail(email, name, token);

        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine, times(1)).process(eq("password-reset"), any(IContext.class));
    }

    @Test
    void testSendWelcomeEmail() {
        // Arrange
        String email = "test@example.com";
        String name = "Test User";

        // Act
        emailService.sendWelcomeEmail(email, name);

        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(templateEngine, times(1)).process(eq("welcome-email"), any(IContext.class));
    }
}