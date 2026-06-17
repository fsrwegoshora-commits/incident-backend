package com.smartincident.incidentbackend.setting.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartincident.incidentbackend.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "agencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agency extends BaseEntity {

    @Column(nullable = false)
    private String code; // POLICE, FIRE, AMBULANCE, ECA — not unique; multiple agencies per type are allowed

    @Column(nullable = false)
    private String name;

    private String description;

    /** Alias exposed in JSON so the frontend can use agencyType consistently. */
    @JsonProperty("agencyType")
    public String getAgencyType() {
        return this.code;
    }
}