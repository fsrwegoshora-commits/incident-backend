package com.smartincident.incidentbackend.emergency.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.DispatchStatus;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_dispatches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDispatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentReport incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private EmergencyVehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatched_by")
    private User dispatchedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DispatchStatus status = DispatchStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime dispatchedAt = LocalDateTime.now();

    @Column
    private LocalDateTime acknowledgedAt;

    @Column
    private LocalDateTime arrivedAt;

    @Column
    private LocalDateTime clearedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Estimated time of arrival in minutes (recalculated as vehicle moves)
    @Column
    private Integer etaMinutes;

    // Denormalized crew snapshot at the time of dispatch (from active VehicleShift)
    @Column(length = 30)
    private String vehiclePlate;

    @Column(length = 150)
    private String driverName;

    @Column(length = 150)
    private String commanderName;

    @Column(columnDefinition = "TEXT")
    private String crewJson;
}
