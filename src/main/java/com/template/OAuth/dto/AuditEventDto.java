package com.template.OAuth.dto;

import com.template.OAuth.enums.AuditEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Data transfer object for audit events")
public class AuditEventDto {

    @Schema(description = "Unique identifier of the audit event")
    private Long id;

    @Schema(description = "Type of the audit event", example = "LOGIN_SUCCESS")
    private AuditEventType type;

    @Schema(description = "Principal (user) who performed the action", example = "user@example.com")
    private String principal;

    @Schema(description = "Description of the event", example = "User login successful")
    private String description;

    @Schema(description = "Timestamp when the event occurred")
    private Instant timestamp;

    @Schema(description = "Additional details about the event (may be JSON)")
    private String details;

    @Schema(description = "IP address from which the action was performed", example = "192.168.1.1")
    private String ipAddress;

    @Schema(description = "User agent information", example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
    private String userAgent;

    @Schema(description = "Source/endpoint where the action was performed", example = "/api/user/profile")
    private String source;

    @Schema(description = "Outcome of the action", example = "SUCCESS")
    private String outcome;
}