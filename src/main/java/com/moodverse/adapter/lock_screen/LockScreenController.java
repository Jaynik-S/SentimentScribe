package com.moodverse.adapter.lock_screen;

import com.moodverse.usecase.verify_password.VerifyPasswordInputBoundary;
import com.moodverse.usecase.verify_password.VerifyPasswordInputData;

public class LockScreenController {
    private final VerifyPasswordInputBoundary verifyPasswordInputInteractor;

    public LockScreenController(VerifyPasswordInputBoundary verifyPasswordInputInteractor) {
        this.verifyPasswordInputInteractor = verifyPasswordInputInteractor;
    }

    public void execute(String password) {
        final VerifyPasswordInputData inputData = new VerifyPasswordInputData(password);
        verifyPasswordInputInteractor.execute(inputData);
    }
}
