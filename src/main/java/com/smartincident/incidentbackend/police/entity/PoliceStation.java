package com.smartincident.incidentbackend.police.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.AreaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "police_stations")
public class PoliceStation extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "contact_info", nullable = false)
    private String contactInfo;

    @ManyToOne
    @JoinColumn(name = "administrative_area_id")
    private AdministrativeArea policeStationLocation;

    @Embedded
    private Location location;

    @Transient
    private Double temporaryDistance;
}
