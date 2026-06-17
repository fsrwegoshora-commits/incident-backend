package com.smartincident.incidentbackend.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OfficerSummaryDto {
    /** User UID — used in the assign-as-admin API call. */
    private String uid;
    private String name;
    /** OfficerRank code (e.g. "ISP", "SGT"). Null for fire/medical officers. */
    private String rank;
    /** Human-readable rank label (e.g. "Inspector"). Null for fire/medical officers. */
    private String rankLabel;
    /** Badge number — police only, null otherwise. */
    private String badgeNumber;
    /** Station or unit name the officer is currently posted to. */
    private String unit;
}
