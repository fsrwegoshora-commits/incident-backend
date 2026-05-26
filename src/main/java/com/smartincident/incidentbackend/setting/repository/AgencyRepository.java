package com.smartincident.incidentbackend.setting.repository;

import com.smartincident.incidentbackend.setting.entity.Agency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AgencyRepository extends JpaRepository<Agency, Long> {
    Optional<Agency> findByUid(String agencyUid);

    @Query("select a from Agency a where (:isActive is null or a.isActive = :isActive) and " +
            "(lower(a.code) like %:key% or lower(a.name) like %:key% or lower(a.description) like %:key%)")
    Page<Agency> gatAgencies(Pageable pageable, Boolean isActive, String key);

    boolean existsByCode(String trim);

    Optional<Agency> findByCode(String code);
}