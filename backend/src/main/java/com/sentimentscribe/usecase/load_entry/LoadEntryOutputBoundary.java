package com.sentimentscribe.usecase.load_entry;

public interface LoadEntryOutputBoundary {
    void prepareSuccessView(LoadEntryOutputData outputData);

    void prepareFailView(String errorMessage);
}
