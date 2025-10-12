package com.smartincident.incidentbackend.police.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.enums.OfficerRank;
import com.smartincident.incidentbackend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name= "police_officers")
public class PoliceOfficer extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String badgeNumber;

    @Enumerated(EnumType.STRING)
    private OfficerRank code;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private PoliceStation station;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User userAccount;
}
