package com.sentimentscribe.usecase.verify_password;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VerifyPasswordInteractor implements VerifyPasswordInputBoundary {
    private static final String DEFAULT_USERNAME = "default";

    private final VerifyPasswordUserDataAccessInterface userDataAccess;
    private final RenderEntriesUserDataInterface renderEntriesDataAccess;
    private final VerifyPasswordOutputBoundary userPresenter;

    public VerifyPasswordInteractor(VerifyPasswordUserDataAccessInterface userDataAccessInterface,
                                   VerifyPasswordOutputBoundary verifyPasswordOutputBoundary,
                                    RenderEntriesUserDataInterface renderEntriesDataAccess) {
        this.userDataAccess = userDataAccessInterface;
        this.userPresenter = verifyPasswordOutputBoundary;
        this.renderEntriesDataAccess = renderEntriesDataAccess;
    }

    @Override
    public void execute(VerifyPasswordInputData inputData) {
        String password = inputData.getPassword();
        try {
            String passwordStatus = userDataAccess.verifyPassword(password);
            if (passwordStatus.equals("Incorrect Password")) {
                userPresenter.prepareFailView("Incorrect Password");
            }
            else {
                UUID userId = userDataAccess.getUserIdByUsername(DEFAULT_USERNAME);
                if (userId == null) {
                    userPresenter.prepareFailView("Failed to verify password: user not found.");
                    return;
                }
                List<Map<String, Object>> allEntries = renderEntriesDataAccess.getAll(userId);
                VerifyPasswordOutputData outputData = new VerifyPasswordOutputData(passwordStatus, allEntries);
                userPresenter.prepareSuccessView(outputData);
            }
        }
        catch (Exception error) {
            userPresenter.prepareFailView("Failed to verify password: " + error.getMessage());
        }
    }
}

