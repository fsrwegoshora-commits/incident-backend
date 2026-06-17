package com.smartincident.incidentbackend.rank.repository;

import com.smartincident.incidentbackend.enums.AgencyType;
import com.smartincident.incidentbackend.rank.entity.AgencyRank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgencyRankRepository extends JpaRepository<AgencyRank, Long> {

    List<AgencyRank> findByAgencyTypeAndIsActiveTrueOrderByRankOrderAsc(AgencyType agencyType);

    Optional<AgencyRank> findByCodeAndAgencyType(String code, AgencyType agencyType);

    boolean existsByAgencyType(AgencyType agencyType);
}
