package com.smartincident.incidentbackend.setting.repository;

import com.smartincident.incidentbackend.setting.entity.AreaLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AreaLevelRepository extends JpaRepository<AreaLevel, Long> {
    Optional<AreaLevel> findByName(String name);
    Optional<AreaLevel> findById(Long id);
}