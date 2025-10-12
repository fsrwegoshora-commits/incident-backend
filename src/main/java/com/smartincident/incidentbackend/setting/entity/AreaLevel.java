package com.smartincident.incidentbackend.setting.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.entity.GrandBaseEntity;
import com.smartincident.incidentbackend.enums.AdministrativeAreaLevel;
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
@Table(name = "area_level")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class AreaLevel extends GrandBaseEntity {

    @Column(name = "id", unique = true)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "name_sw")
    private String nameSw;

    @Enumerated(EnumType.STRING)
    private AdministrativeAreaLevel level;
}

