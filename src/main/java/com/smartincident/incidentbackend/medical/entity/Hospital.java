package com.smartincident.incidentbackend.medical.entity;

import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.HospitalType;
import com.smartincident.incidentbackend.police.entity.Location;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "hospitals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospital extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HospitalType hospitalType;

    @Embedded
    private Location location;

    /** Total bed capacity of the hospital. */
    @Column
    private Integer bedCapacity;

    /** Direct emergency / ambulance dispatch phone number. */
    @Column(length = 100)
    private String emergencyContact;

    /**
     * The EmergencyUnit (type = HOSPITAL_AMBULANCE_UNIT) responsible for
     * coordinating ambulance dispatch from this hospital.
     * Nullable — some hospitals may not yet have a registered ambulance unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_unit_id", unique = true)
    private EmergencyUnit ambulanceUnit;
}
