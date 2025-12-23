package com.sentimentscribe.persistence.postgres;

import com.sentimentscribe.persistence.postgres.entity.UserEntity;
import com.sentimentscribe.persistence.postgres.repo.UserJpaRepository;
import com.sentimentscribe.usecase.verify_password.VerifyPasswordUserDataAccessInterface;
import java.time.LocalDateTime;
import java.util.Optional;

public class PostgresVerifyPasswordDataAccessObject implements VerifyPasswordUserDataAccessInterface {
    private static final String DEFAULT_USERNAME = "default";

    private final UserJpaRepository userRepository;

    public PostgresVerifyPasswordDataAccessObject(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String verifyPassword(String password) throws Exception {
        String candidate = password == null ? "" : password;
        Optional<UserEntity> existing = userRepository.findByUsername(DEFAULT_USERNAME);
        if (existing.isEmpty()) {
            UserEntity user = new UserEntity();
            user.setUsername(DEFAULT_USERNAME);
            user.setPassword(candidate);
            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userRepository.save(user);
            return "Created new password.";
        }

        UserEntity user = existing.get();
        String storedPassword = user.getPassword();
        if (storedPassword != null && storedPassword.equals(candidate)) {
            return "Correct Password";
        }
        if (storedPassword == null || storedPassword.isEmpty()) {
            user.setPassword(candidate);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return "Created new password.";
        }
        return "Incorrect Password";
    }
}
