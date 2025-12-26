package com.sentimentscribe.service;

import com.sentimentscribe.persistence.postgres.entity.UserEntity;
import com.sentimentscribe.persistence.postgres.repo.UserJpaRepository;
import com.sentimentscribe.web.dto.AuthTokenResponse;
import com.sentimentscribe.web.dto.E2eeParamsResponse;
import com.sentimentscribe.web.dto.UserResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final String DEFAULT_E2EE_KDF = "PBKDF2-SHA256";
    private static final int DEFAULT_E2EE_ITERATIONS = 310000;
    private static final int DEFAULT_E2EE_SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserJpaRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public ServiceResult<AuthTokenResponse> register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername == null) {
            return ServiceResult.failure("Username is required.");
        }
        if (password == null || password.isBlank()) {
            return ServiceResult.failure("Password is required.");
        }
        Optional<UserEntity> existing = userRepository.findByUsername(normalizedUsername);
        if (existing.isPresent()) {
            return ServiceResult.failure("Username already exists.");
        }
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setE2eeKdf(DEFAULT_E2EE_KDF);
        user.setE2eeSalt(generateSalt());
        user.setE2eeIterations(DEFAULT_E2EE_ITERATIONS);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        UserEntity saved = userRepository.save(user);
        return ServiceResult.success(buildAuthResponse(saved));
    }

    public ServiceResult<AuthTokenResponse> login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername == null || password == null) {
            return ServiceResult.failure("Invalid username or password.");
        }
        Optional<UserEntity> existing = userRepository.findByUsername(normalizedUsername);
        if (existing.isEmpty()) {
            return ServiceResult.failure("Invalid username or password.");
        }
        UserEntity user = existing.get();
        String storedHash = user.getPasswordHash();
        if (storedHash == null || !passwordEncoder.matches(password, storedHash)) {
            return ServiceResult.failure("Invalid username or password.");
        }
        return ServiceResult.success(buildAuthResponse(user));
    }

    private AuthTokenResponse buildAuthResponse(UserEntity user) {
        String accessToken = jwtService.issueToken(user.getId(), user.getUsername());
        String salt = Base64.getEncoder().encodeToString(user.getE2eeSalt());
        UserResponse userResponse = new UserResponse(user.getId().toString(), user.getUsername());
        E2eeParamsResponse e2ee = new E2eeParamsResponse(
                user.getE2eeKdf(),
                salt,
                user.getE2eeIterations()
        );
        return new AuthTokenResponse(accessToken, "Bearer", jwtService.getTtlSeconds(), userResponse, e2ee);
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[DEFAULT_E2EE_SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

}
