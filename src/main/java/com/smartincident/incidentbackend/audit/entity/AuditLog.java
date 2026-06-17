package com.smartincident.incidentbackend.audit.entity;

import com.smartincident.incidentbackend.utils.Utils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor_uid"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_uid"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, length = 50)
    private String uid = Utils.generateUniqueID();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "actor_uid", length = 50)
    private String actorUid;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "actor_role", length = 30)
    private String actorRole;

    /** CREATE, UPDATE, DELETE, LOGIN, LOGOUT */
    @Column(nullable = false, length = 30)
    private String action;

    /** e.g. User, IncidentReport, PoliceStation */
    @Column(name = "entity_type", length = 80)
    private String entityType;

    @Column(name = "entity_uid", length = 50)
    private String entityUid;

    @Column(length = 250)
    private String endpoint;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** true = operation completed successfully */
    @Builder.Default
    @Column(nullable = false)
    private boolean success = true;

    /** Previous value of changed field (for UPDATE/DELETE actions). */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** New value after the change (for CREATE/UPDATE actions). */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** High-level audit category for filtering: AUTH, INCIDENT, USER, RESOURCE, CONFIG. */
    @Column(name = "category", length = 30)
    private String category;
}
