package com.smartincident.incidentbackend.gis.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.GeofenceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "geofences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Geofence extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GeofenceType type;

    /** Centre of the circular geofence. */
    @Column(nullable = false)
    private Double centerLatitude;

    @Column(nullable = false)
    private Double centerLongitude;

    /** Radius in metres. */
    @Column(nullable = false)
    private Double radiusMetres;

    /** Optional: agency this geofence belongs to. Null = system-wide. */
    @Column
    private String agencyUid;

    @Column(nullable = false)
    private Boolean alertOnEntry = true;

    @Column(nullable = false)
    private Boolean alertOnExit = false;
}
