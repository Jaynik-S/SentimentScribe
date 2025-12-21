package com.sentimentscribe.data;

import com.sentimentscribe.config.AuthProperties;
import com.sentimentscribe.usecase.verify_password.VerifyPasswordUserDataAccessInterface;

public class VerifyPasswordDataAccessObject implements VerifyPasswordUserDataAccessInterface {
    public String passwordStatus;

    private static String sysPassword;

    public VerifyPasswordDataAccessObject(AuthProperties authProperties) {
        String configured = authProperties.password();
        if (configured != null && !configured.isBlank()) {
            sysPassword = configured;
        }
    }

    public static void setSysPasswordForTesting(String password) {
        sysPassword = password;
    }

    public String verifyPassword(String password) throws Exception {
        if (sysPassword != null && sysPassword.equals(password)) {
            passwordStatus = "Correct Password";
        }
        else if (sysPassword == null || sysPassword.isEmpty()) {
            sysPassword = password;
            passwordStatus = "Created new password.";
        }
        else {
            passwordStatus = "Incorrect Password";
        }
        return passwordStatus;
    }
}
