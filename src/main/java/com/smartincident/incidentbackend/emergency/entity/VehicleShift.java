package com.smartincident.incidentbackend.emergency.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.ShiftTime;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.TrafficCheckpoint;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicle_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleShift extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private EmergencyVehicle vehicle;

    @Column(nullable = false)
    private LocalDate shiftDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftTime shiftTime;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(columnDefinition = "TEXT")
    private String dutyDescription;

    // Crew assigned to this vehicle for this shift
    @ManyToMany
    @JoinTable(
        name = "vehicle_shift_crew",
        joinColumns = @JoinColumn(name = "shift_id"),
        inverseJoinColumns = @JoinColumn(name = "officer_id")
    )
    @Builder.Default
    private List<PoliceOfficer> crew = new ArrayList<>();

    // Optional standby post (reuse TrafficCheckpoint as standby location)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standby_location_id")
    private TrafficCheckpoint standbyLocation;
}
