package com.smartincident.incidentbackend.fire.repository;

import com.smartincident.incidentbackend.fire.entity.FireCrew;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FireCrewRepository extends JpaRepository<FireCrew, Long> {

    Optional<FireCrew> findByUid(String uid);

    @Query("""
        select c from FireCrew c
        where c.unit.uid = :unitUid
          and (:isActive is null or c.isActive = :isActive)
          and (lower(c.name) like %:key% or lower(coalesce(c.coverageArea,'')) like %:key%)
        order by c.createdAt desc
        """)
    Page<FireCrew> findByUnit(Pageable pageable, Boolean isActive, String key,
                              @Param("unitUid") String unitUid);
}
