package com.template.OAuth.audit;

import com.template.OAuth.entities.AuditEvent;
import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.repositories.AuditEventRepository;
import com.template.OAuth.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuditLoggingTests {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    public void testAuditLogging() {
        String testPrincipal = "test@example.com";
        String testDescription = "Test audit event";

        auditService.logEvent(new AuditEvent(AuditEventType.SYSTEM_EVENT, testPrincipal, testDescription));

        // Allow async processing to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<AuditEvent> events = auditEventRepository.findByPrincipalOrderByTimestampDesc(testPrincipal,
                PageRequest.of(0, 10)).getContent();

        assertFalse(events.isEmpty(), "Audit events list should not be empty");
        AuditEvent event = events.get(0);
        assertEquals(AuditEventType.SYSTEM_EVENT, event.getType(), "Event type should match");
        assertEquals(testPrincipal, event.getPrincipal(), "Principal should match");
        assertEquals(testDescription, event.getDescription(), "Description should match");
    }
}