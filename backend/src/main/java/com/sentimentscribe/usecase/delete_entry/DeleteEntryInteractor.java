package com.sentimentscribe.usecase.delete_entry;

public class DeleteEntryInteractor implements DeleteEntryInputBoundary {

    private final DeleteEntryOutputBoundary presenter;
    private final DeleteEntryUserDataAccessInterface dataAccess;

    public DeleteEntryInteractor(DeleteEntryOutputBoundary presenter, DeleteEntryUserDataAccessInterface dataAccess) {
        this.presenter = presenter;
        this.dataAccess = dataAccess;
    }

    @Override
    public void execute(DeleteEntryInputData inputData) {
        if (inputData.getUserId() == null) {
            presenter.prepareFailView("User is required.");
            return;
        }
        String entryPath = inputData.getEntryPath();

        if (entryPath == null || entryPath.length() == 0) {
            presenter.prepareFailView("Entry path cannot be empty.");
            return;
        }
        try {
            boolean deleted = dataAccess.deleteByPath(inputData.getUserId(), entryPath);
            if (!deleted) {
                presenter.prepareFailView("Failed to delete entry: file not found.");
                return;
            }
        }
        catch (Exception error) {
            String message = "Failed to delete entry: " + error.getMessage();
            presenter.prepareFailView(message);
            return;
        }
        DeleteEntryOutputData outputData = new DeleteEntryOutputData(true, entryPath);

        presenter.prepareSuccessView(outputData);
    }

}

