package com.moodverse.usecase.delete_entry;

public interface DeleteEntryOutputBoundary {
    void prepareSuccessView(DeleteEntryOutputData outputData);

    void prepareFailView(String errorMessage);
}
