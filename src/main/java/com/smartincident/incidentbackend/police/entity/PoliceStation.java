package com.smartincident.incidentbackend.police.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.StationLevel;
import com.smartincident.incidentbackend.enums.StationServiceType;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private StationLevel level = StationLevel.POLICE_STATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private StationServiceType serviceType = StationServiceType.POLICE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_station_id")
    @JsonIgnoreProperties({"parentStation", "policeStationLocation", "temporaryDistance"})
    private PoliceStation parentStation;

    @ManyToOne
    @JoinColumn(name = "administrative_area_id")
    private AdministrativeArea policeStationLocation;

    @Embedded
    private Location location;

    @Transient
    private Double temporaryDistance;
}
