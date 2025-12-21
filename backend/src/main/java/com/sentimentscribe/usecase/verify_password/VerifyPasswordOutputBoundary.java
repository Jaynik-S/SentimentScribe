package com.sentimentscribe.usecase.verify_password;

public interface VerifyPasswordOutputBoundary {
    void prepareSuccessView(VerifyPasswordOutputData outputData);

    void prepareFailView(String errorMessage);
}

