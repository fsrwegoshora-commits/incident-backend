package com.smartincident.incidentbackend.authotp.entity;

import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CITIZEN;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private PoliceStation station;
}
