package com.moodverse.service;

import com.moodverse.data.DiaryEntryRepository;
import com.moodverse.usecase.delete_entry.DeleteEntryInputData;
import com.moodverse.usecase.delete_entry.DeleteEntryInteractor;
import com.moodverse.usecase.delete_entry.DeleteEntryOutputBoundary;
import com.moodverse.usecase.delete_entry.DeleteEntryOutputData;
import com.moodverse.usecase.load_entry.LoadEntryInputData;
import com.moodverse.usecase.load_entry.LoadEntryInteractor;
import com.moodverse.usecase.load_entry.LoadEntryOutputBoundary;
import com.moodverse.usecase.load_entry.LoadEntryOutputData;
import com.moodverse.usecase.save_entry.SaveEntryInputData;
import com.moodverse.usecase.save_entry.SaveEntryInteractor;
import com.moodverse.usecase.save_entry.SaveEntryKeywordExtractor;
import com.moodverse.usecase.save_entry.SaveEntryOutputBoundary;
import com.moodverse.usecase.save_entry.SaveEntryOutputData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EntryService {

    private final DiaryEntryRepository repository;
    private final SaveEntryKeywordExtractor keywordExtractor;

    public EntryService(DiaryEntryRepository repository, SaveEntryKeywordExtractor keywordExtractor) {
        this.repository = repository;
        this.keywordExtractor = keywordExtractor;
    }

    public ServiceResult<SaveEntryOutputData> save(EntryCommand command) {
        SaveEntryPresenter presenter = new SaveEntryPresenter();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, repository, keywordExtractor);
        SaveEntryInputData inputData = new SaveEntryInputData(
                command.title(),
                command.createdAt(),
                command.text(),
                command.storagePath(),
                command.keywords()
        );
        interactor.execute(inputData);
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    public ServiceResult<LoadEntryOutputData> load(String entryPath) {
        LoadEntryPresenter presenter = new LoadEntryPresenter();
        LoadEntryInteractor interactor = new LoadEntryInteractor(presenter, repository);
        interactor.execute(new LoadEntryInputData(entryPath));
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    public ServiceResult<DeleteEntryOutputData> delete(String entryPath) {
        DeleteEntryPresenter presenter = new DeleteEntryPresenter();
        DeleteEntryInteractor interactor = new DeleteEntryInteractor(presenter, repository);
        interactor.execute(new DeleteEntryInputData(entryPath));
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    public ServiceResult<List<Map<String, Object>>> list() {
        try {
            return ServiceResult.success(repository.getAll());
        }
        catch (Exception error) {
            return ServiceResult.failure("Failed to load entries: " + error.getMessage());
        }
    }

    private static final class SaveEntryPresenter implements SaveEntryOutputBoundary {
        private SaveEntryOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(SaveEntryOutputData outputData) {
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    private static final class LoadEntryPresenter implements LoadEntryOutputBoundary {
        private LoadEntryOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(LoadEntryOutputData outputData) {
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    private static final class DeleteEntryPresenter implements DeleteEntryOutputBoundary {
        private DeleteEntryOutputData outputData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(DeleteEntryOutputData outputData) {
            this.outputData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
