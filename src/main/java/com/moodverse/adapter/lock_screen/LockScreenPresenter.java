package com.moodverse.adapter.lock_screen;

import com.moodverse.adapter.ViewManagerModel;
import com.moodverse.adapter.home_menu.HomeMenuPresenter;
import com.moodverse.adapter.home_menu.HomeMenuViewModel;
import com.moodverse.usecase.verify_password.VerifyPasswordOutputBoundary;
import com.moodverse.usecase.verify_password.VerifyPasswordOutputData;

public class LockScreenPresenter implements VerifyPasswordOutputBoundary {

    private final LockScreenViewModel lockScreenViewModel;
    private final ViewManagerModel viewManagerModel;
    private final HomeMenuViewModel homeMenuViewModel;
    private final HomeMenuPresenter homeMenuPresenter;

    public LockScreenPresenter(LockScreenViewModel lockScreenViewModel,
                               ViewManagerModel viewManagerModel,
                               HomeMenuViewModel homeMenuViewModel,
                               HomeMenuPresenter homeMenuPresenter) {
        this.lockScreenViewModel = lockScreenViewModel;
        this.viewManagerModel = viewManagerModel;
        this.homeMenuViewModel = homeMenuViewModel;
        this.homeMenuPresenter = homeMenuPresenter;
    }

    public void prepareSuccessView(VerifyPasswordOutputData outputData) {
        if (homeMenuPresenter != null && outputData != null) {
            homeMenuPresenter.presentEntriesFromData(outputData.getAllEntries());
        }
        final LockScreenState state = lockScreenViewModel.getState();
        state.setError(null);
        lockScreenViewModel.firePropertyChanged();

        this.viewManagerModel.setState(homeMenuViewModel.getViewName());
        this.viewManagerModel.firePropertyChanged();
    }

    public void prepareFailView(String errorMessage) {
        final LockScreenState state = lockScreenViewModel.getState();
        state.setError(errorMessage);
        lockScreenViewModel.firePropertyChanged();
    }
}
