package com.moodverse.adapter.new_document;

import com.moodverse.adapter.ViewManagerModel;
import com.moodverse.usecase.analyze_keywords.AnalyzeKeywordsOutputBoundary;
import com.moodverse.usecase.analyze_keywords.AnalyzeKeywordsOutputData;
import com.moodverse.usecase.create_entry.CreateEntryOutputBoundary;
import com.moodverse.usecase.create_entry.CreateEntryOutputData;
import com.moodverse.usecase.load_entry.LoadEntryOutputBoundary;
import com.moodverse.usecase.load_entry.LoadEntryOutputData;
import com.moodverse.usecase.save_entry.SaveEntryOutputBoundary;
import com.moodverse.usecase.save_entry.SaveEntryOutputData;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NewDocumentPresenter implements
        SaveEntryOutputBoundary,
        LoadEntryOutputBoundary,
        CreateEntryOutputBoundary,
        AnalyzeKeywordsOutputBoundary {

    private final NewDocumentViewModel newDocumentViewModel;
    private final ViewManagerModel viewManagerModel;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy h:mm a");

    public NewDocumentPresenter(NewDocumentViewModel newDocumentViewModel, ViewManagerModel viewManagerModel) {
        this.newDocumentViewModel = newDocumentViewModel;
        this.viewManagerModel = viewManagerModel;
    }

    @Override
    public void prepareSuccessView(CreateEntryOutputData outputData) {
        final NewDocumentState state = newDocumentViewModel.getState();
        state.setTitle(outputData.getTitle());
        state.setTextBody(outputData.getText());
        state.setKeywords(List.of());
        state.setStoragePath(null);

        if (outputData.getDate() != null) {
            state.setDate(outputData.getDate().format(formatter));
        }
        else {
            state.setDate("");
        }

        state.setError(null);
        state.setSuccessMessage(null);

        newDocumentViewModel.setState(state);
        newDocumentViewModel.firePropertyChanged();

        viewManagerModel.setState(newDocumentViewModel.getViewName());
        viewManagerModel.firePropertyChanged();
    }

    @Override
    public void prepareSuccessView(LoadEntryOutputData outputData) {
        final NewDocumentState state = newDocumentViewModel.getState();
        state.setTitle(outputData.getTitle());
        state.setTextBody(outputData.getText());
        state.setKeywords(outputData.getKeywords() == null ? List.of() : outputData.getKeywords());
        state.setStoragePath(outputData.getStoragePath());

        if (outputData.getDate() != null) {
            state.setDate(outputData.getDate().format(formatter));
        }
        else {
            state.setDate("");
        }

        state.setError(null);
        state.setSuccessMessage(null);

        newDocumentViewModel.setState(state);
        newDocumentViewModel.firePropertyChanged();

        viewManagerModel.setState(newDocumentViewModel.getViewName());
        viewManagerModel.firePropertyChanged();
    }

    @Override
    public void prepareSuccessView(SaveEntryOutputData outputData) {
        final NewDocumentState state = newDocumentViewModel.getState();
        state.setSuccessMessage("Document saved successfully");

        state.setTitle(outputData.getTitle());
        state.setTextBody(outputData.getText());
        state.setStoragePath(outputData.getStoragePath());
        state.setKeywords(outputData.getKeywords() == null ? List.of() : outputData.getKeywords());

        if (outputData.getDate() != null) {
            state.setDate(outputData.getDate().format(formatter));
        }

        state.setError(null);

        newDocumentViewModel.setState(state);
        newDocumentViewModel.firePropertyChanged();
    }

    @Override
    public void prepareSuccessView(AnalyzeKeywordsOutputData outputData) {
        final NewDocumentState state = newDocumentViewModel.getState();
        state.setKeywords(outputData.getKeywords());
        state.setError(null);
        state.setSuccessMessage(null);

        newDocumentViewModel.setState(state);
        newDocumentViewModel.firePropertyChanged("keywords");
    }

    @Override
    public void prepareFailView(String errorMessage) {
        final NewDocumentState state = newDocumentViewModel.getState();
        state.setError(errorMessage);
        newDocumentViewModel.firePropertyChanged();
    }

}

