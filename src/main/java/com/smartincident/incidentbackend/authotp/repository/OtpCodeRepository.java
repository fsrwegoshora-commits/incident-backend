package com.smartincident.incidentbackend.authotp.repository;

import com.smartincident.incidentbackend.authotp.entity.OtpCode;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode,Long> {
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.phoneNumber = :phoneNumber and o.isActive=true")
    @Transactional
    int deleteByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT o FROM OtpCode o WHERE o.phoneNumber=:phoneNumber and o.code=:code and o.isActive=true")
    Optional<OtpCode> findByPhoneNumberAndCode(String phoneNumber, String code);

    @Query("SELECT o FROM OtpCode o WHERE o.phoneNumber = :phoneNumber")
    Optional<OtpCode> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT o FROM OtpCode o WHERE o.phoneNumber = :phoneNumber and o.isActive=true")
    List<OtpCode> findAllByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
