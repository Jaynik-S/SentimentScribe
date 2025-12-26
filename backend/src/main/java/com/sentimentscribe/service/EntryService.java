package com.sentimentscribe.service;

import com.sentimentscribe.data.DiaryEntryRepository;
import com.sentimentscribe.usecase.delete_entry.DeleteEntryInputData;
import com.sentimentscribe.usecase.delete_entry.DeleteEntryInteractor;
import com.sentimentscribe.usecase.delete_entry.DeleteEntryOutputBoundary;
import com.sentimentscribe.usecase.delete_entry.DeleteEntryOutputData;
import com.sentimentscribe.usecase.load_entry.LoadEntryInputData;
import com.sentimentscribe.usecase.load_entry.LoadEntryInteractor;
import com.sentimentscribe.usecase.load_entry.LoadEntryOutputBoundary;
import com.sentimentscribe.usecase.load_entry.LoadEntryOutputData;
import com.sentimentscribe.usecase.save_entry.SaveEntryInputData;
import com.sentimentscribe.usecase.save_entry.SaveEntryInteractor;
import com.sentimentscribe.usecase.save_entry.SaveEntryOutputBoundary;
import com.sentimentscribe.usecase.save_entry.SaveEntryOutputData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EntryService {

    private final DiaryEntryRepository repository;
    public EntryService(DiaryEntryRepository repository) {
        this.repository = repository;
    }

    public ServiceResult<SaveEntryOutputData> save(UUID userId, EntryCommand command) {
        SaveEntryPresenter presenter = new SaveEntryPresenter();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, repository);
        SaveEntryInputData inputData = new SaveEntryInputData(
                userId,
                command.titleCiphertext(),
                command.titleIv(),
                command.bodyCiphertext(),
                command.bodyIv(),
                command.algo(),
                command.version(),
                command.storagePath(),
                command.createdAt()
        );
        interactor.execute(inputData);
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    public ServiceResult<LoadEntryOutputData> load(UUID userId, String entryPath) {
        LoadEntryPresenter presenter = new LoadEntryPresenter();
        LoadEntryInteractor interactor = new LoadEntryInteractor(presenter, repository);
        interactor.execute(new LoadEntryInputData(userId, entryPath));
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    public ServiceResult<DeleteEntryOutputData> delete(UUID userId, String entryPath) {
        DeleteEntryPresenter presenter = new DeleteEntryPresenter();
        DeleteEntryInteractor interactor = new DeleteEntryInteractor(presenter, repository);
        interactor.execute(new DeleteEntryInputData(userId, entryPath));
        if (presenter.errorMessage != null) {
            return ServiceResult.failure(presenter.errorMessage);
        }
        return ServiceResult.success(presenter.outputData);
    }

    public ServiceResult<List<Map<String, Object>>> list(UUID userId) {
        try {
            return ServiceResult.success(repository.getAll(userId));
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
