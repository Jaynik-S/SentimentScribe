package com.moodverse.usecase.verify_password;

public interface VerifyPasswordOutputBoundary {
    void prepareSuccessView(VerifyPasswordOutputData outputData);

    void prepareFailView(String errorMessage);
}

