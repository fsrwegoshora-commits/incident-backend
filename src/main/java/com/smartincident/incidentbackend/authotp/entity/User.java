package com.smartincident.incidentbackend.authotp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.enums.DispatcherAppointment;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.setting.entity.Agency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Data
@Table(name="users")
public class User extends BaseEntity {


    @Column(unique = true)
    private String phoneNumber;

    /** Unique username for management users (ROOT, AGENCY_ADMIN, etc.). Null for citizens. */
    @Column(unique = true)
    private String username;

    /** BCrypt-hashed password for management users. Null for citizens (who use OTP). */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "name",nullable = false)
    private String name;

    private boolean verified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CITIZEN;

    /**
     * The EmergencyUnit this user is assigned to.
     * Used for Fire and Medical roles (fire officer, medic, fire/medical station admin).
     * For Police roles, use policeStation instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_unit_id")
    @JsonIgnoreProperties({"parentUnit", "administrativeArea", "temporaryDistance"})
    private EmergencyUnit emergencyUnit;

    /**
     * The PoliceStation this user belongs to.
     * Used for Police roles (POLICE_OFFICER, STATION_ADMIN).
     * Null for fire and medical users.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "police_station_id")
    @JsonIgnoreProperties({"parentStation", "policeStationLocation", "temporaryDistance"})
    private PoliceStation policeStation;

    /** Appointment/rank within the dispatch center hierarchy. Applies to DISPATCH_CENTER_ADMIN, DISPATCHER_SUPERVISOR, DISPATCHER. */
    @Enumerated(EnumType.STRING)
    @Column(name = "dispatcher_appointment")
    private DispatcherAppointment appointment;

    /** ISO 639-1 language code for UI and notifications: "en" (English) or "sw" (Swahili). Defaults to English. */
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en";

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", uid='" + getUid() + '\'' +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                // OMIT emergencyUnit
                '}';
    }
}
