package com.smartincident.incidentbackend.authotp.service;

import com.smartincident.incidentbackend.authotp.dto.AuthResponse;
import com.smartincident.incidentbackend.authotp.entity.OtpCode;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.OtpCodeRepository;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.utils.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.otp.expose-in-response:true}")
    private boolean exposeOtpInResponse;

    @Value("${app.otp.rate-limit-seconds:60}")
    private long rateLimitSeconds;

    // In-memory rate limiter: phone → last request timestamp (ms)
    private final Map<String, Long> otpRequestTimes = new ConcurrentHashMap<>();

    @Transactional
    public Response<String> generateOtp(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return Response.error("Phone number is required");
        }

        String phone = phoneNumber.trim();

        if (!userRepository.existsByPhoneNumberAndIsActiveTrue(phone)) {
            return Response.error("Phone number not registered");
        }

        // Rate limiting
        Long lastRequest = otpRequestTimes.get(phone);
        if (lastRequest != null) {
            long elapsed = System.currentTimeMillis() - lastRequest;
            long rateLimitMs = rateLimitSeconds * 1000;
            if (elapsed < rateLimitMs) {
                long secondsLeft = (rateLimitMs - elapsed) / 1000 + 1;
                return Response.error("Please wait " + secondsLeft + " seconds before requesting a new OTP");
            }
        }

        try {
            otpCodeRepository.deleteByPhoneNumber(phone);

            String otp = String.valueOf(100000 + new Random().nextInt(900000));
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

            otpCodeRepository.save(new OtpCode(phone, otp, expiry));
            otpRequestTimes.put(phone, System.currentTimeMillis());

            log.info("OTP generated for: {}", phone);

            // Send via SMS — wire up SmsService here when ready
            // smsService.sendSms(phone, "Your verification code is: " + otp + ". Valid for 5 minutes.");

            // Only expose OTP in response during development
            String responseData = exposeOtpInResponse ? otp : "OTP sent to your phone";
            return Response.success(responseData);

        } catch (Exception e) {
            log.error("Failed to generate OTP for {}: {}", phone, e.getMessage());
            return Response.error("Failed to generate OTP");
        }
    }

    public Response<Boolean> verifyOtp(String phoneNumber, String code) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return Response.error("Phone number is required");
        }
        if (code == null || code.trim().isEmpty()) {
            return Response.error("OTP code is required");
        }

        Optional<OtpCode> otpOpt = otpCodeRepository.findByPhoneNumberAndCode(phoneNumber.trim(), code.trim());
        if (otpOpt.isEmpty()) {
            return Response.error("Invalid OTP");
        }

        OtpCode otp = otpOpt.get();
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpCodeRepository.delete(otp);
            return Response.error("OTP has expired");
        }

        Optional<User> optUser = userRepository.findByPhoneNumber(phoneNumber.trim());
        if (optUser.isEmpty()) {
            return Response.error("User not found");
        }

        User user = optUser.get();
        user.setVerified(true);
        userRepository.save(user);
        otpCodeRepository.delete(otp);
        otpRequestTimes.remove(phoneNumber.trim());

        return new Response<>(true);
    }

    /**
     * Verifies OTP and returns JWT access + refresh tokens.
     * Throws RuntimeException with a descriptive message on any failure.
     */
    public AuthResponse loginWithOtp(String phoneNumber, String code) {
        Response<Boolean> otpResponse = verifyOtp(phoneNumber, code);
        if (!otpResponse.success() || Boolean.FALSE.equals(otpResponse.getData())) {
            throw new RuntimeException(otpResponse.getMessage());
        }

        User user = userRepository.findByPhoneNumber(phoneNumber.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getPhoneNumber(),
                user.getRole(),
                user.getStation() != null ? user.getStation().getUid() : null
        );
    }
}
