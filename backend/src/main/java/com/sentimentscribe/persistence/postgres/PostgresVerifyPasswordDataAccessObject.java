package com.sentimentscribe.persistence.postgres;

import com.sentimentscribe.persistence.postgres.entity.UserEntity;
import com.sentimentscribe.persistence.postgres.repo.UserJpaRepository;
import com.sentimentscribe.usecase.verify_password.VerifyPasswordUserDataAccessInterface;
import java.time.LocalDateTime;
import java.util.Optional;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PostgresVerifyPasswordDataAccessObject implements VerifyPasswordUserDataAccessInterface {
    private static final String DEFAULT_USERNAME = "default";
    private static final String DEFAULT_E2EE_KDF = "PBKDF2-SHA256";
    private static final int DEFAULT_E2EE_ITERATIONS = 310000;
    private static final int DEFAULT_E2EE_SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PostgresVerifyPasswordDataAccessObject(UserJpaRepository userRepository,
                                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String verifyPassword(String password) throws Exception {
        String candidate = password == null ? "" : password;
        Optional<UserEntity> existing = userRepository.findByUsername(DEFAULT_USERNAME);
        if (existing.isEmpty()) {
            UserEntity user = new UserEntity();
            user.setUsername(DEFAULT_USERNAME);
            user.setPasswordHash(passwordEncoder.encode(candidate));
            applyE2eeDefaults(user);
            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userRepository.save(user);
            return "Created new password.";
        }

        UserEntity user = existing.get();
        String storedPasswordHash = user.getPasswordHash();
        if (storedPasswordHash != null && passwordEncoder.matches(candidate, storedPasswordHash)) {
            return "Correct Password";
        }
        if (storedPasswordHash == null || storedPasswordHash.isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(candidate));
            applyE2eeDefaults(user);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return "Created new password.";
        }
        return "Incorrect Password";
    }

    private void applyE2eeDefaults(UserEntity user) {
        if (user.getE2eeKdf() == null || user.getE2eeKdf().isBlank()) {
            user.setE2eeKdf(DEFAULT_E2EE_KDF);
        }
        if (user.getE2eeSalt() == null || user.getE2eeSalt().length == 0) {
            user.setE2eeSalt(generateSalt());
        }
        if (user.getE2eeIterations() <= 0) {
            user.setE2eeIterations(DEFAULT_E2EE_ITERATIONS);
        }
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[DEFAULT_E2EE_SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }
}
