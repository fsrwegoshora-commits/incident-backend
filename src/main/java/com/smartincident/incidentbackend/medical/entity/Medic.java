package com.smartincident.incidentbackend.medical.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.MedicRole;
import jakarta.persistence.*;
import lombok.*;

/**
 * Ambulance crew member attached to a hospital's ambulance unit.
 *
 * In Tanzania each hospital manages its own ambulance fleet and medical crew.
 * A Medic is assigned to a {@link EmergencyUnit} of type HOSPITAL_AMBULANCE_UNIT
 * and belongs to a specific {@link Hospital}.
 *
 * Roles range from Emergency Medical Technician (EMT) through paramedic to
 * nurse / clinical officer riding as crew.
 */
@Entity
@Table(name = "medics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medic extends BaseEntity {

    /** Staff ID / service number issued by the hospital. */
    @Column(unique = true, nullable = false)
    private String staffNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicRole role;

    /** The hospital this medic works for. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    /**
     * The EmergencyUnit (HOSPITAL_AMBULANCE_UNIT) this medic is assigned to
     * for dispatch purposes.  Should match hospital.ambulanceUnit.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_unit_id", nullable = false)
    private EmergencyUnit emergencyUnit;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User userAccount;

    @Column(nullable = false)
    private Boolean isOnDuty = false;
}
