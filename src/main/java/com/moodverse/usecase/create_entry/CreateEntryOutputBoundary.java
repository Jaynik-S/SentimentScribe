package com.moodverse.usecase.create_entry;

public interface CreateEntryOutputBoundary {
    void prepareSuccessView(CreateEntryOutputData outputData);

    void prepareFailView(String errorMessage);
}
