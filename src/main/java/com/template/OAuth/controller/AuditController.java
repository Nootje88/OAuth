package com.template.OAuth.controller;

import com.template.OAuth.annotation.Auditable;
import com.template.OAuth.dto.AuditEventDto;
import com.template.OAuth.entities.AuditEvent;
import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/audit")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Tag(name = "Audit", description = "Audit log management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @Operation(summary = "Get all audit events",
            description = "Retrieves all audit events with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit events retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @Auditable(type = AuditEventType.DATA_ACCESSED, description = "Accessed audit logs")
    @GetMapping
    public ResponseEntity<Page<AuditEventDto>> getAllAuditEvents(
            @Parameter(description = "Page number (zero-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditEvent> auditEvents = auditService.findAllEvents(pageable);

        Page<AuditEventDto> auditEventDtos = auditEvents.map(this::convertToDto);
        return ResponseEntity.ok(auditEventDtos);
    }

    @Operation(summary = "Get audit events by user",
            description = "Retrieves audit events for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit events retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @Auditable(type = AuditEventType.DATA_ACCESSED, description = "Accessed user audit logs")
    @GetMapping("/user/{principal}")
    public ResponseEntity<Page<AuditEventDto>> getAuditEventsByUser(
            @Parameter(description = "User principal (email)", required = true)
            @PathVariable String principal,
            @Parameter(description = "Page number (zero-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditEvent> auditEvents = auditService.findEventsByPrincipal(principal, pageable);

        Page<AuditEventDto> auditEventDtos = auditEvents.map(this::convertToDto);
        return ResponseEntity.ok(auditEventDtos);
    }

    @Operation(summary = "Get audit events by type",
            description = "Retrieves audit events of a specific type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit events retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @Auditable(type = AuditEventType.DATA_ACCESSED, description = "Accessed type-filtered audit logs")
    @GetMapping("/type/{type}")
    public ResponseEntity<Page<AuditEventDto>> getAuditEventsByType(
            @Parameter(description = "Audit event type", required = true)
            @PathVariable AuditEventType type,
            @Parameter(description = "Page number (zero-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditEvent> auditEvents = auditService.findEventsByType(type, pageable);

        Page<AuditEventDto> auditEventDtos = auditEvents.map(this::convertToDto);
        return ResponseEntity.ok(auditEventDtos);
    }

    @Operation(summary = "Get audit events by date range",
            description = "Retrieves audit events within a specific date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit events retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @Auditable(type = AuditEventType.DATA_ACCESSED, description = "Accessed date-filtered audit logs")
    @GetMapping("/date-range")
    public ResponseEntity<Page<AuditEventDto>> getAuditEventsByDateRange(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @Parameter(description = "Page number (zero-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        Instant startInstant = start.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atZone(ZoneId.systemDefault()).toInstant();

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditEvent> auditEvents = auditService.findEventsByTimeRange(startInstant, endInstant, pageable);

        Page<AuditEventDto> auditEventDtos = auditEvents.map(this::convertToDto);
        return ResponseEntity.ok(auditEventDtos);
    }

    @Operation(summary = "Search audit events",
            description = "Searches audit events by description or details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit events retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @Auditable(type = AuditEventType.DATA_ACCESSED, description = "Searched audit logs")
    @GetMapping("/search")
    public ResponseEntity<Page<AuditEventDto>> searchAuditEvents(
            @Parameter(description = "Search term", required = true)
            @RequestParam String term,
            @Parameter(description = "Page number (zero-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditEvent> auditEvents = auditService.searchEvents(term, pageable);

        Page<AuditEventDto> auditEventDtos = auditEvents.map(this::convertToDto);
        return ResponseEntity.ok(auditEventDtos);
    }

    @Operation(summary = "Get available audit event types",
            description = "Retrieves all available audit event types")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event types retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content)
    })
    @GetMapping("/types")
    public ResponseEntity<List<String>> getAuditEventTypes() {
        return ResponseEntity.ok(
                java.util.Arrays.stream(AuditEventType.values())
                        .map(Enum::name)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Convert AuditEvent entity to DTO
     */
    private AuditEventDto convertToDto(AuditEvent auditEvent) {
        AuditEventDto dto = new AuditEventDto();
        dto.setId(auditEvent.getId());
        dto.setType(auditEvent.getType());
        dto.setPrincipal(auditEvent.getPrincipal());
        dto.setDescription(auditEvent.getDescription());
        dto.setTimestamp(auditEvent.getTimestamp());
        dto.setDetails(auditEvent.getDetails());
        dto.setIpAddress(auditEvent.getIpAddress());
        dto.setUserAgent(auditEvent.getUserAgent());
        dto.setSource(auditEvent.getSource());
        dto.setOutcome(auditEvent.getOutcome());
        return dto;
    }

}