package com.smartincident.incidentbackend.entity;

import com.smartincident.incidentbackend.utils.LoggedUser;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.time.LocalDateTime;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity extends GrandBaseEntity {

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.createdBy = LoggedUser.getUid();
    }

    public void update() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = LoggedUser.getUid();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = LoggedUser.getUid(); // salama hapa
        this.isActive = false;
        this.isDeleted = true;
    }

    public void activate() {
        this.isActive = true;
        this.isDeleted = false;
    }
}

