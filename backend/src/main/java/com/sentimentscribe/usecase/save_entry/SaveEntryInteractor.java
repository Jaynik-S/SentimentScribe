package com.sentimentscribe.usecase.save_entry;

import com.sentimentscribe.domain.DiaryEntry;

import java.util.List;

public class SaveEntryInteractor implements SaveEntryInputBoundary {

    private final SaveEntryOutputBoundary presenter;
    private final SaveEntryUserDataAccessInterface dataAccess;
    private final SaveEntryKeywordExtractor keywordExtractor;

    public SaveEntryInteractor(SaveEntryOutputBoundary presenter,
                              SaveEntryUserDataAccessInterface dataAccess,
                              SaveEntryKeywordExtractor keywordExtractor) {
        this.presenter = presenter;
        this.dataAccess = dataAccess;
        this.keywordExtractor = keywordExtractor;
    }

    @Override
    public void execute(SaveEntryInputData inputData) {
        if (inputData.getUserId() == null) {
            presenter.prepareFailView("User is required.");
            return;
        }

        DiaryEntry entry = new DiaryEntry(inputData.getTitle(), inputData.getTextBody(), inputData.getDate());
        entry.setStoragePath(inputData.getStoragePath());

        String title = entry.getTitle();
        if (title == null || title.length() == 0) {
            presenter.prepareFailView("Title cannot be empty.");
            return;
        }
        if (title.length() > DiaryEntry.MAX_TITLE_LENGTH) {
            String message = "Title must be at most " + DiaryEntry.MAX_TITLE_LENGTH + " characters.";
            presenter.prepareFailView(message);
            return;
        }
        String text = entry.getText();
        if (text == null || text.length() == 0) {
            presenter.prepareFailView("Text cannot be empty.");
            return;
        }
        int length = text.length();
        if (length < DiaryEntry.MIN_TEXT_LENGTH) {
            String message = "Text must be at least " + DiaryEntry.MIN_TEXT_LENGTH + " characters.";
            presenter.prepareFailView(message);
            return;
        }
        if (length > DiaryEntry.MAX_TEXT_LENGTH) {
            String message = "Text must be at most " + DiaryEntry.MAX_TEXT_LENGTH + " characters.";
            presenter.prepareFailView(message);
            return;
        }

        List<String> keywords = inputData.getKeywords();
        if ((keywords == null || keywords.isEmpty()) && keywordExtractor != null) {
            try {
                keywords = keywordExtractor.extractKeywords(text);
            }
            catch (Exception ignored) {
                keywords = List.of();
            }
        }
        entry.setKeywords(keywords);

        entry.updatedTime();
        try {
            dataAccess.save(inputData.getUserId(), entry);
        }
        catch (Exception error) {
            String message = "Could not save entry." + error.getMessage();
            presenter.prepareFailView(message);
            return;
        }

        SaveEntryOutputData outputData = new SaveEntryOutputData(
                entry.getTitle(),
                entry.getText(),
                entry.getCreatedAt(),
                entry.getStoragePath(),
                entry.getKeywords(),
                true);

        presenter.prepareSuccessView(outputData);
    }
}

