package com.sentimentscribe.usecase.save_entry;

import com.sentimentscribe.domain.DiaryEntry;
import java.time.LocalDateTime;

public class SaveEntryInteractor implements SaveEntryInputBoundary {

    private final SaveEntryOutputBoundary presenter;
    private final SaveEntryUserDataAccessInterface dataAccess;

    public SaveEntryInteractor(SaveEntryOutputBoundary presenter,
                              SaveEntryUserDataAccessInterface dataAccess) {
        this.presenter = presenter;
        this.dataAccess = dataAccess;
    }

    @Override
    public void execute(SaveEntryInputData inputData) {
        if (inputData.getUserId() == null) {
            presenter.prepareFailView("User is required.");
            return;
        }

        if (isBlank(inputData.getTitleCiphertext())) {
            presenter.prepareFailView("Title ciphertext is required.");
            return;
        }
        if (isBlank(inputData.getTitleIv())) {
            presenter.prepareFailView("Title IV is required.");
            return;
        }
        if (isBlank(inputData.getBodyCiphertext())) {
            presenter.prepareFailView("Body ciphertext is required.");
            return;
        }
        if (isBlank(inputData.getBodyIv())) {
            presenter.prepareFailView("Body IV is required.");
            return;
        }
        if (isBlank(inputData.getAlgo())) {
            presenter.prepareFailView("Algorithm is required.");
            return;
        }
        if (inputData.getVersion() <= 0) {
            presenter.prepareFailView("Version must be positive.");
            return;
        }

        LocalDateTime createdAt = inputData.getCreatedAt() != null
                ? inputData.getCreatedAt()
                : LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        DiaryEntry entry = new DiaryEntry(
                inputData.getTitleCiphertext(),
                inputData.getTitleIv(),
                inputData.getBodyCiphertext(),
                inputData.getBodyIv(),
                inputData.getAlgo(),
                inputData.getVersion(),
                inputData.getStoragePath(),
                createdAt,
                updatedAt
        );

        try {
            dataAccess.save(inputData.getUserId(), entry);
        }
        catch (Exception error) {
            String message = "Could not save entry." + error.getMessage();
            presenter.prepareFailView(message);
            return;
        }

        SaveEntryOutputData outputData = new SaveEntryOutputData(
                entry.getTitleCiphertext(),
                entry.getTitleIv(),
                entry.getBodyCiphertext(),
                entry.getBodyIv(),
                entry.getAlgo(),
                entry.getVersion(),
                entry.getStoragePath(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                true
        );

        presenter.prepareSuccessView(outputData);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

