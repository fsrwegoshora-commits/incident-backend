package com.smartincident.incidentbackend.authotp.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="otp_Code")
public class OtpCode extends BaseEntity {
    private String phoneNumber;
    private String code;
    private LocalDateTime expiresAt;
}
