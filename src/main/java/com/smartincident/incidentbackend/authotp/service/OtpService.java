package com.smartincident.incidentbackend.authotp.service;

import com.smartincident.incidentbackend.authotp.dto.AuthResponse;
import com.smartincident.incidentbackend.authotp.entity.OtpCode;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.OtpCodeRepository;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.notification.service.SmsService;
import com.smartincident.incidentbackend.permission.service.PermissionService;
import com.smartincident.incidentbackend.session.service.DeviceSessionService;
import com.smartincident.incidentbackend.utils.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PermissionService permissionService;
    private final DeviceSessionService deviceSessionService;
    private final SmsService smsService;

    @Value("${app.otp.expose-in-response:false}")
    private boolean exposeOtpInResponse;

    @Value("${app.otp.rate-limit-seconds:60}")
    private long rateLimitSeconds;

    @Value("${app.otp.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.otp.lockout-minutes:15}")
    private long lockoutMinutes;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // In-memory rate limiter: phone → last OTP request timestamp (ms)
    private final Map<String, Long> otpRequestTimes = new ConcurrentHashMap<>();
    // Brute-force protection: phone → failed verification attempts
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    // Lockout tracker: phone → lockout expiry timestamp (ms)
    private final Map<String, Long> lockoutUntil = new ConcurrentHashMap<>();

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

            String otp = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

            otpCodeRepository.save(new OtpCode(phone, otp, expiry));
            otpRequestTimes.put(phone, System.currentTimeMillis());

            boolean smsSent = smsService.sendSms(phone,
                    "Your verification code is: " + otp + ". Valid for 5 minutes.");
            log.info("OTP generated for {} (smsSent={})", phone, smsSent);

            // Only expose OTP in response during development
            String responseData = exposeOtpInResponse ? otp : "OTP sent to your phone";
            return Response.success(responseData);

        } catch (Exception e) {
            log.error("Failed to generate OTP for {}: {}", phone, e.getMessage());
            return Response.error("Failed to generate OTP");
        }
    }

    public Response<Boolean> verifyOtp(String phoneNumber, String code) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty())
            return Response.error("Phone number is required");
        if (code == null || code.trim().isEmpty())
            return Response.error("OTP code is required");

        String phone = phoneNumber.trim();

        // Brute-force lockout check
        Long lockedUntil = lockoutUntil.get(phone);
        if (lockedUntil != null && System.currentTimeMillis() < lockedUntil) {
            long secs = (lockedUntil - System.currentTimeMillis()) / 1000 + 1;
            return Response.error("Account temporarily locked due to too many failed attempts. Try again in " + secs + " seconds.");
        }

        Optional<OtpCode> otpOpt = otpCodeRepository.findByPhoneNumberAndCode(phone, code.trim());
        if (otpOpt.isEmpty()) {
            int attempts = failedAttempts.computeIfAbsent(phone, k -> new AtomicInteger(0)).incrementAndGet();
            if (attempts >= maxFailedAttempts) {
                lockoutUntil.put(phone, System.currentTimeMillis() + lockoutMinutes * 60 * 1000);
                failedAttempts.remove(phone);
                log.warn("OTP lockout triggered for phone: {}", phone);
                return Response.error("Too many failed attempts. Account locked for " + lockoutMinutes + " minutes.");
            }
            return Response.error("Invalid OTP");
        }

        OtpCode otp = otpOpt.get();
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpCodeRepository.delete(otp);
            failedAttempts.remove(phone);
            return Response.error("OTP has expired");
        }

        Optional<User> optUser = userRepository.findByPhoneNumber(phone);
        if (optUser.isEmpty())
            return Response.error("User not found");

        User user = optUser.get();
        user.setVerified(true);
        userRepository.save(user);
        otpCodeRepository.delete(otp);
        otpRequestTimes.remove(phone);
        failedAttempts.remove(phone);
        lockoutUntil.remove(phone);

        return new Response<>(true);
    }

    /**
     * Verifies OTP and returns JWT access + refresh tokens.
     * Throws RuntimeException with a descriptive message on any failure.
     */
    public AuthResponse loginWithOtp(String phoneNumber, String code) {
        return loginWithOtp(phoneNumber, code, null, null, null, null);
    }

    public AuthResponse loginWithOtp(String phoneNumber, String code,
                                     String deviceId, String deviceName,
                                     String platform, String ipAddress) {
        Response<Boolean> otpResponse = verifyOtp(phoneNumber, code);
        if (!otpResponse.success() || Boolean.FALSE.equals(otpResponse.getData())) {
            throw new RuntimeException(otpResponse.getMessage());
        }

        User user = userRepository.findByPhoneNumber(phoneNumber.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // OTP login is exclusively for citizens — management staff must use /api/auth/login
        if (user.getRole() != null && user.getRole() != com.smartincident.incidentbackend.enums.Role.CITIZEN) {
            throw new RuntimeException("Management accounts must log in with username and password at /api/auth/login");
        }

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        Set<Permission> permissions = permissionService.getPermissionsForRole(user.getRole());

        // Async — never blocks login
        deviceSessionService.recordLogin(
                user.getUid(), user.getName(),
                user.getRole() != null ? user.getRole().name() : null,
                deviceId, deviceName, platform, ipAddress);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getPhoneNumber(),
                user.getRole(),
                user.getEmergencyUnit() != null ? user.getEmergencyUnit().getUid() : null,
                permissions
        );
    }
}
