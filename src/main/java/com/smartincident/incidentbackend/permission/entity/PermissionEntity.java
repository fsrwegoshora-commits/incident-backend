package com.smartincident.incidentbackend.permission.entity;

import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.PermissionCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 50)
    private Permission name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PermissionCategory category;
}
