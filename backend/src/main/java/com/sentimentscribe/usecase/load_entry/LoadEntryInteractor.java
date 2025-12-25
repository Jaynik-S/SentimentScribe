package com.sentimentscribe.usecase.load_entry;

import com.sentimentscribe.domain.DiaryEntry;

public class LoadEntryInteractor implements LoadEntryInputBoundary {
    private final LoadEntryOutputBoundary presenter;
    private final LoadEntryUserDataAccessInterface dataAccess;

    public LoadEntryInteractor(LoadEntryOutputBoundary presenter, LoadEntryUserDataAccessInterface dataAccess) {
        this.presenter = presenter;
        this.dataAccess = dataAccess;
    }

    @Override
    public void execute(LoadEntryInputData inputData) {
        if (inputData.getUserId() == null) {
            presenter.prepareFailView("User is required.");
            return;
        }
        String entryPath = inputData.getEntryPath();

        if (entryPath == null || entryPath.length() == 0) {
            presenter.prepareFailView("Entry path cannot be empty.");
            return;
        }
        DiaryEntry entry;
        try {
            entry = dataAccess.getByPath(inputData.getUserId(), entryPath);
        }
        catch (Exception error) {
            String message = "Failed to load entry: " + error.getMessage();
            presenter.prepareFailView(message);
            return;
        }
        if (entry == null) {
            String message = "Failed to load entry from path: " + entryPath;
            presenter.prepareFailView(message);
            return;
        }

        LoadEntryOutputData outputData = new LoadEntryOutputData(
                entry.getTitle(),
                entry.getText(),
                entry.getCreatedAt(),
                entry.getStoragePath() == null || entry.getStoragePath().isBlank() ? entryPath : entry.getStoragePath(),
                entry.getKeywords(),
                true);

        presenter.prepareSuccessView(outputData);
    }
}

