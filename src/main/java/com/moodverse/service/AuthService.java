package com.moodverse.service;

import com.moodverse.usecase.verify_password.RenderEntriesUserDataInterface;
import com.moodverse.usecase.verify_password.VerifyPasswordInputData;
import com.moodverse.usecase.verify_password.VerifyPasswordInteractor;
import com.moodverse.usecase.verify_password.VerifyPasswordOutputBoundary;
import com.moodverse.usecase.verify_password.VerifyPasswordOutputData;
import com.moodverse.usecase.verify_password.VerifyPasswordUserDataAccessInterface;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final VerifyPasswordUserDataAccessInterface passwordAccess;
    private final RenderEntriesUserDataInterface entriesAccess;

    public AuthService(VerifyPasswordUserDataAccessInterface passwordAccess,
                       RenderEntriesUserDataInterface entriesAccess) {
        this.passwordAccess = passwordAccess;
        this.entriesAccess = entriesAccess;
    }

    public ServiceResult<VerifyPasswordOutputData> verifyPassword(String password) {
        VerifyPresenter presenter = new VerifyPresenter();
        VerifyPasswordInteractor interactor = new VerifyPasswordInteractor(passwordAccess, presenter, entriesAccess);
        interactor.execute(new VerifyPasswordInputData(password));
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    private static final class VerifyPresenter implements VerifyPasswordOutputBoundary {
        private VerifyPasswordOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(VerifyPasswordOutputData outputData) {
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
