package com.smartincident.incidentbackend.setting.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.entity.GrandBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "area_type")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class AreaType extends GrandBaseEntity {

    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "name_sw")
    private String nameSw;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "area_level_id")
    private AreaLevel areaLevel;

    @Column(name = "name_plural")
    private String namePlural;

    @Column(name = "name_plural_sw")
    private String namePluralSw;
}
