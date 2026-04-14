package com.smartincident.incidentbackend.emergency.entity;

import com.smartincident.incidentbackend.entity.GrandBaseEntity;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_locations",
       indexes = @Index(name = "idx_vehicle_locations_vehicle_recorded",
                        columnList = "vehicle_id, recorded_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLocation extends GrandBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private EmergencyVehicle vehicle;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column
    private Double speedKmh;

    @Column
    private Double heading;   // degrees 0–360

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    // Non-null when vehicle is actively responding to an incident
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private IncidentReport incident;
}
