package com.moodverse.usecase.verify_password;

public class VerifyPasswordInputData {
    private final String password;

    public VerifyPasswordInputData(String password) {
        this.password = password;
    }

    String getPassword() {
        return password;
    }
}

