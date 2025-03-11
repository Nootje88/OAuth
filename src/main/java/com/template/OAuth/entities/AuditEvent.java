package com.template.OAuth.entities;

import com.template.OAuth.enums.AuditEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEventType type;

    @Column(nullable = false)
    private String principal;

    @Column(nullable = false)
    private String description;

    @CreationTimestamp
    private Instant timestamp;

    @Column(length = 2000)
    private String details;

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    @Column
    private String source;

    @Column
    private String outcome;

    // Helper constructor for common use cases
    public AuditEvent(AuditEventType type, String principal, String description) {
        this.type = type;
        this.principal = principal;
        this.description = description;
    }
}