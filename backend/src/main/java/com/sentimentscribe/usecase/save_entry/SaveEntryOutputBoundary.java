package com.sentimentscribe.usecase.save_entry;

public interface SaveEntryOutputBoundary {
    void prepareSuccessView(SaveEntryOutputData outputData);

    void prepareFailView(String errorMessage);
}


