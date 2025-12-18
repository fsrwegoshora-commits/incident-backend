package com.smartincident.incidentbackend.police.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.setting.entity.Department;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "traffic_checkpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficCheckpoint extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Embedded
    private Location location;

    private String contactPhone;

    private Double coverageRadiusKm;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private PoliceStation parentStation;

    @ManyToOne
    @JoinColumn(name = "supervising_officer_id")
    private PoliceOfficer supervisingOfficer;

    private Boolean active = true;
}
