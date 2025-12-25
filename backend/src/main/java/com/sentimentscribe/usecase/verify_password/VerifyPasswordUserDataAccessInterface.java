package com.sentimentscribe.usecase.verify_password;

import java.util.UUID;

public interface VerifyPasswordUserDataAccessInterface {
    String verifyPassword(String password) throws Exception;

    UUID getUserIdByUsername(String username) throws Exception;
}

