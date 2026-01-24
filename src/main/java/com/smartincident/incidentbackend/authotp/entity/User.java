package com.smartincident.incidentbackend.authotp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "name",nullable = false)
    private String name;

    private boolean verified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CITIZEN;

    @ManyToOne
    @JoinColumn(name = "station_id")
    @JsonIgnore
    private PoliceStation station;


    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", uid='" + getUid() + '\'' +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                // OMIT station
                '}';
    }
}
