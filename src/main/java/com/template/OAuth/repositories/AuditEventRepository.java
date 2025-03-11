package com.template.OAuth.repositories;

import com.template.OAuth.entities.AuditEvent;
import com.template.OAuth.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    // Find events by principal (user identifier)
    Page<AuditEvent> findByPrincipalOrderByTimestampDesc(String principal, Pageable pageable);

    // Find events by type
    Page<AuditEvent> findByTypeOrderByTimestampDesc(AuditEventType type, Pageable pageable);

    // Find events by time range
    Page<AuditEvent> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end, Pageable pageable);

    // Find events by principal and time range
    Page<AuditEvent> findByPrincipalAndTimestampBetweenOrderByTimestampDesc(
            String principal, Instant start, Instant end, Pageable pageable);

    // Find events by type and time range
    Page<AuditEvent> findByTypeAndTimestampBetweenOrderByTimestampDesc(
            AuditEventType type, Instant start, Instant end, Pageable pageable);

    // Find events by principal and type
    Page<AuditEvent> findByPrincipalAndTypeOrderByTimestampDesc(
            String principal, AuditEventType type, Pageable pageable);

    // Find events by principal, type, and time range
    Page<AuditEvent> findByPrincipalAndTypeAndTimestampBetweenOrderByTimestampDesc(
            String principal, AuditEventType type, Instant start, Instant end, Pageable pageable);

    // Search in description or details
    @Query("SELECT a FROM AuditEvent a WHERE " +
            "(LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.details) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditEvent> searchAuditEvents(@Param("searchTerm") String searchTerm, Pageable pageable);
}