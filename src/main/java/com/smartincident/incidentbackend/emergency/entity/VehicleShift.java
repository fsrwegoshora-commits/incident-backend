package com.smartincident.incidentbackend.emergency.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.ShiftTime;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.medical.entity.Medic;
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

    // ── Police vehicle crew (PoliceOfficer) ────────────────────────────────
    @ManyToMany
    @JoinTable(
        name = "vehicle_shift_crew",
        joinColumns = @JoinColumn(name = "shift_id"),
        inverseJoinColumns = @JoinColumn(name = "officer_id")
    )
    @Builder.Default
    private List<PoliceOfficer> crew = new ArrayList<>();

    /** Standby post for police/fire shifts (TrafficCheckpoint or similar). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standby_location_id")
    private TrafficCheckpoint standbyLocation;

    // ── Ambulance-specific crew ────────────────────────────────────────────

    /** Medics assigned to this ambulance shift. */
    @ManyToMany
    @JoinTable(
        name = "vehicle_shift_medics",
        joinColumns = @JoinColumn(name = "shift_id"),
        inverseJoinColumns = @JoinColumn(name = "medic_id")
    )
    @Builder.Default
    @JsonIgnoreProperties({"hospital", "emergencyUnit", "hibernateLazyInitializer"})
    private List<Medic> medics = new ArrayList<>();

    /**
     * The medic responsible for this shift.
     * Their phone is used for real-time location tracking during incidents.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_charge_medic_id")
    @JsonIgnoreProperties({"hospital", "emergencyUnit", "hibernateLazyInitializer"})
    private Medic inCharge;

    /** The designated driver for this ambulance shift. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_medic_id")
    @JsonIgnoreProperties({"hospital", "emergencyUnit", "hibernateLazyInitializer"})
    private Medic driver;

    /**
     * The hospital or base where the ambulance will be stationed during this shift.
     * Null for fire / police shifts.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standby_hospital_id")
    @JsonIgnoreProperties({"ambulanceUnit", "location", "hibernateLazyInitializer"})
    private Hospital standbyHospital;

    /**
     * Free-text description of the response point / standby location
     * when no Hospital entity is applicable.
     */
    @Column(length = 200)
    private String standbyLocationName;
}
