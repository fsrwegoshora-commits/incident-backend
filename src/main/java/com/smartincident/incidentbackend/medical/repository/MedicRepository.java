package com.smartincident.incidentbackend.medical.repository;

import com.smartincident.incidentbackend.medical.entity.Medic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MedicRepository extends JpaRepository<Medic, Long> {

    Optional<Medic> findByUid(String uid);

    Optional<Medic> findByUserAccountUid(String userUid);

    boolean existsByUserAccountUid(String userUid);

    List<Medic> findByUidIn(List<String> uids);

    List<Medic> findByHospitalUidAndIsActiveTrue(String hospitalUid);

    List<Medic> findByEmergencyUnitUidAndIsActiveTrue(String unitUid);

    @Query("""
        select m from Medic m
        where lower(concat(m.userAccount.name, m.staffNumber)) like %:key%
          and (:isActive is null or m.isActive = :isActive)
          and (:hospitalUid is null or m.hospital.uid = :hospitalUid)
        order by m.role asc
        """)
    Page<Medic> search(Pageable pageable, Boolean isActive, String key, String hospitalUid);
}
