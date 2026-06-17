package com.smartincident.incidentbackend.operational.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.PostStatus;
import com.smartincident.incidentbackend.enums.PostType;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.police.entity.Location;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.setting.entity.Agency;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "operational_posts", indexes = {
    @Index(name = "idx_op_post_agency",  columnList = "agency_id"),
    @Index(name = "idx_op_post_type",    columnList = "post_type"),
    @Index(name = "idx_op_post_status",  columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationalPost extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    /** Parent police station — set for POLICE_PATROL_POST and POLICE_CHECKPOINT. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_station_id")
    private PoliceStation parentStation;

    /** Parent fire/medical EmergencyUnit — set for FIRE_OPERATIONAL_POST and MEDICAL_OPERATIONAL_POST. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id")
    private EmergencyUnit parentUnit;

    /** Parent hospital — set for MEDICAL_OPERATIONAL_POST when linked to a hospital. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_hospital_id")
    private Hospital parentHospital;

    @Embedded
    private Location location;

    /** Service radius in kilometres. */
    private Double coverageRadiusKm;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
