package com.smartincident.incidentbackend.fire.repository;

import com.smartincident.incidentbackend.fire.entity.FireOfficer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FireOfficerRepository extends JpaRepository<FireOfficer, Long> {

    Optional<FireOfficer> findByUid(String uid);

    Optional<FireOfficer> findByUserAccountUid(String userUid);

    boolean existsByUserAccountUid(String userUid);

    /** All active officers at a fire unit, ordered by rank. */
    @Query("""
        select f from FireOfficer f
        where f.emergencyUnit.uid = :unitUid and f.isActive = true
        order by case f.rank
          when 'CFO' then 1 when 'DCFO' then 2 when 'SDO' then 3
          when 'DO' then 4 when 'ADO' then 5 when 'SO' then 6
          when 'ASO' then 7 when 'SUB_OFFICER' then 8
          when 'LEADING_FM' then 9 else 10 end asc
        """)
    List<FireOfficer> findByUnitUidOrderedByRank(String unitUid);

    @Query("""
        select f from FireOfficer f
        where lower(concat(f.userAccount.name, f.serviceNumber)) like %:key%
          and (:isActive is null or f.isActive = :isActive)
          and (:unitUid is null or f.emergencyUnit.uid = :unitUid)
        order by f.rank asc
        """)
    Page<FireOfficer> search(Pageable pageable, Boolean isActive, String key, String unitUid);
}
