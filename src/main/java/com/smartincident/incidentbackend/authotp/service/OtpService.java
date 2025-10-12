package com.smartincident.incidentbackend.authotp.service;

import com.smartincident.incidentbackend.authotp.entity.OtpCode;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.OtpCodeRepository;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.utils.Response;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@GraphQLApi
public class OtpService {
    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional
    public Response<String> generateOtp(String phoneNumber) {
        log.info("Generating OTP for phone number: {}", phoneNumber);

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.error("Phone number is null or empty");
            return Response.error("Phone number is required");
        }

        String cleanedPhoneNumber = phoneNumber.trim();

        // Check if user exists
        boolean userExists = userRepository.existsByPhoneNumberAndIsActiveTrue(cleanedPhoneNumber);
        log.debug("User exists check for {}: {}", cleanedPhoneNumber, userExists);

        if (!userExists) {
            log.error("Phone number {} not registered", cleanedPhoneNumber);
            return Response.error("Phone number not registered or deleted");
        }

        try {
            // Check existing OTPs first
            List<OtpCode> existingOtps = otpCodeRepository.findAllByPhoneNumber(cleanedPhoneNumber);
            log.debug("Found {} existing OTPs for {}", existingOtps.size(), cleanedPhoneNumber);

            // Delete existing OTPs
            int deletedCount = otpCodeRepository.deleteByPhoneNumber(cleanedPhoneNumber);
            log.debug("Deleted {} OTPs for {}", deletedCount, cleanedPhoneNumber);

            // Generate new OTP
            String otp = String.valueOf(100000 + new Random().nextInt(900000));
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(2);

            OtpCode otpCode = new OtpCode(cleanedPhoneNumber, otp, expiry);
            OtpCode savedOtp = otpCodeRepository.save(otpCode);

            log.info("OTP saved with ID: {} for phone number: {}", savedOtp.getId(), cleanedPhoneNumber);
            log.info("Simulated SMS sent to {} with OTP: {}", cleanedPhoneNumber, otp);

            return Response.success(otp);

        } catch (Exception e) {
            log.error("Failed to process OTP for phone number {}: {}", cleanedPhoneNumber, e.getMessage());
            e.printStackTrace();
            return Response.error("Failed to generate OTP");
        }
    }

    public Response<Boolean> verifyOtp(String phoneNumber, String code) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.error("Phone number is null or empty");
            return Response.error("Phone number is required");
        }
        if (code == null || code.trim().isEmpty()) {
            log.error("OTP code is null or empty");
            return Response.error("OTP code is required");
        }

        log.info("Verifying OTP for phone number: {}, code: {}", phoneNumber, code);
        Optional<OtpCode> otpOpt = otpCodeRepository.findByPhoneNumberAndCode(phoneNumber, code);
        if (otpOpt.isEmpty()) {
            log.warn("No OTP found for phone number: {}, code: {}", phoneNumber, code);
            return Response.error("Invalid OTP");
        }

        OtpCode otp = otpOpt.get();
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for phone number: {}, code: {}", phoneNumber, code);
            otpCodeRepository.delete(otp); // Clean up expired OTP
            return Response.error("OTP is expired");
        }

        Optional<User> optUser = userRepository.findByPhoneNumber(phoneNumber);
        if (optUser.isEmpty()) {
            log.error("User not found for phone number: {}", phoneNumber);
            return Response.error("User does not exist");
        }

        User user = optUser.get();
        user.setVerified(true);
        try {
            userRepository.save(user);
            log.info("User verified for phone number: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to save user verification for phone number {}: {}", phoneNumber, e.getMessage());
            return Response.error("Failed to verify user");
        }

        try {
            otpCodeRepository.delete(otp);
            log.info("OTP deleted for phone number: {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to delete OTP for phone number {}: {}", phoneNumber, e.getMessage());
            // Continue, as user verification is already saved
        }

        return new Response<>(true);
    }

    public String loginWithOtp(String phoneNumber, String code) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.error("Phone number is null or empty in loginWithOtp");
        }
        if (code == null || code.trim().isEmpty()) {
            log.error("OTP code is null or empty in loginWithOtp");
        }

        log.info("Logging in with OTP for phone number: {}, code: {}", phoneNumber, code);

        Response<Boolean> otpResponse = verifyOtp(phoneNumber, code);
        if (!otpResponse.success() || !otpResponse.getData()) {
            log.warn("OTP verification failed for phone number: {}, message: {}", phoneNumber, otpResponse.getMessage());
            return otpResponse.getMessage();
        }

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> {
                    log.error("User not found after OTP verification for phone number: {}", phoneNumber);
                    return new RuntimeException("User not found");
                });

        try {
            String token = jwtService.generateToken(user);
            log.info("JWT token generated for phone number: {}", phoneNumber);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate JWT token for phone number {}: {}", phoneNumber, e.getMessage());
            return null;
        }

    }
}