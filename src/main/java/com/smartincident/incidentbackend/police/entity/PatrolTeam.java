package com.smartincident.incidentbackend.police.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.PatrolTeamStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patrol_teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatrolTeam extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "agency", "parentStation", "policeStationLocation"})
    private PoliceStation station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "policeStation"})
    private PoliceVehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_leader_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "policeStation", "department", "userAccount"})
    private PoliceOfficer teamLeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "policeStation", "department", "userAccount"})
    private PoliceOfficer driver;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "patrol_team_members",
        joinColumns = @JoinColumn(name = "patrol_team_id"),
        inverseJoinColumns = @JoinColumn(name = "officer_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "policeStation", "department"})
    @Builder.Default
    private List<PoliceOfficer> members = new ArrayList<>();

    @Column(name = "coverage_area")
    private String coverageArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatrolTeamStatus status = PatrolTeamStatus.STANDBY;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
