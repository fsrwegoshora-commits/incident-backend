package com.smartincident.incidentbackend.setting.entity;

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

    @Column(nullable = false, unique = true)
    private String code; // POLICE, FIRE, AMBULANCE

    @Column(nullable = false)
    private String name;

    private String description;
}