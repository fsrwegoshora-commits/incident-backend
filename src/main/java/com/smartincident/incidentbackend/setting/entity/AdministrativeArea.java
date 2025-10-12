package com.smartincident.incidentbackend.setting.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.entity.GrandBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "administrative_areas")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class AdministrativeArea extends GrandBaseEntity {

    @Column(name = "id", nullable = true, unique = true)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "name", nullable = true)
    private String name;

    @Column(name = "parent_area_id")
    private Long parentAreaId;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "area_type_id")
    private AreaType areaType;

    @Column(name = "label")
    private String label;

    public AdministrativeArea(Long id) {

        setId(id);
    }

}

