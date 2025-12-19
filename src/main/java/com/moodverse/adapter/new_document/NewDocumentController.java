package com.moodverse.adapter.new_document;

import com.moodverse.usecase.analyze_keywords.AnalyzeKeywordsInputBoundary;
import com.moodverse.usecase.analyze_keywords.AnalyzeKeywordsInputData;
import com.moodverse.usecase.get_recommendations.GetRecommendationsInputBoundary;
import com.moodverse.usecase.get_recommendations.GetRecommendationsInputData;
import com.moodverse.usecase.go_back.GoBackInputBoundary;
import com.moodverse.usecase.save_entry.SaveEntryInputBoundary;
import com.moodverse.usecase.save_entry.SaveEntryInputData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NewDocumentController {

    private final GetRecommendationsInputBoundary getRecommendationsInteractor;
    private final GoBackInputBoundary goBackInteractor;
    private final SaveEntryInputBoundary saveEntryInteractor;
    private final AnalyzeKeywordsInputBoundary analyzeKeywordsInteractor;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy h:mm a");

    public NewDocumentController(GetRecommendationsInputBoundary getRecommendationsInteractor,
                                 GoBackInputBoundary goBackInteractor,
                                 SaveEntryInputBoundary saveEntryInteractor,
                                 AnalyzeKeywordsInputBoundary analyzeKeywordsInteractor) {
        this.getRecommendationsInteractor = getRecommendationsInteractor;
        this.goBackInteractor = goBackInteractor;
        this.saveEntryInteractor = saveEntryInteractor;
        this.analyzeKeywordsInteractor = analyzeKeywordsInteractor;
    }

    public void executeSave(String title,
                            String dateString,
                            String textBody,
                            String storagePath,
                            List<String> keywords) {
        LocalDateTime date;
        if (dateString == null || dateString.isEmpty()) {
            date = LocalDateTime.now();
        }
        else {
            try {
                date = LocalDateTime.parse(dateString, formatter);
            }
            catch (Exception error) {
                date = LocalDateTime.now();
            }
        }

        final SaveEntryInputData inputData = new SaveEntryInputData(title, date, textBody, storagePath, keywords);

        saveEntryInteractor.execute(inputData);
    }

    public void executeBack() {
        goBackInteractor.execute();
    }

    public void executeGetRecommendations(String textBody) {
        final GetRecommendationsInputData inputData = new GetRecommendationsInputData(textBody);
        getRecommendationsInteractor.execute(inputData);
    }

    public void executeAnalyzeKeywords(String textBody) {
        final AnalyzeKeywordsInputData inputData = new AnalyzeKeywordsInputData(textBody);
        analyzeKeywordsInteractor.execute(inputData);
    }
}

