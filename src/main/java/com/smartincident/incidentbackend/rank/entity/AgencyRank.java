package com.smartincident.incidentbackend.rank.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.AgencyType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stores the official rank hierarchy for POLICE and FIRE agencies.
 * These records are system-defined and seeded at startup — not user-editable.
 */
@Entity
@Table(name = "agency_ranks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"agency_type","code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyRank extends BaseEntity {

    /** Unique code used as a key (e.g. IGP, CP, SGT, CFO). */
    @Column(nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "agency_type", nullable = false, length = 30)
    private AgencyType agencyType;

    @Column(name = "name_english", nullable = false)
    private String nameEnglish;

    @Column(name = "name_swahili")
    private String nameSwahili;

    @Column(name = "abbreviation", length = 20)
    private String abbreviation;

    /** Seniority order — 1 = most senior. */
    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** True for records seeded by the system — these cannot be deleted by admins. */
    @Column(name = "is_system_defined", nullable = false)
    private Boolean isSystemDefined = true;
}
