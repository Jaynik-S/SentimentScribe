package com.sentimentscribe.usecase.load_entry;

import com.sentimentscribe.domain.DiaryEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadEntryInteractorTest {

    // Verifies a valid path reaches the DAO and returns the loaded entry to the presenter.
    @Test
    void execute_withValidPath_returnsEntryToPresenter() throws Exception {
        RecordingLoadEntryPresenter presenter = new RecordingLoadEntryPresenter();
        StubLoadEntryDataAccess dataAccess = new StubLoadEntryDataAccess();
        DiaryEntry entry = new DiaryEntry();
        entry.setTitleCiphertext("VGVzdCBUaXRsZQ==");
        entry.setTitleIv("AAAAAAAAAAAAAAAAAAAAAA==");
        entry.setBodyCiphertext("VGVzdCBCb2R5");
        entry.setBodyIv("AAAAAAAAAAAAAAAAAAAAAA==");
        entry.setAlgo("AES-GCM");
        entry.setVersion(1);
        dataAccess.setEntryToReturn(entry);
        LoadEntryInteractor interactor = new LoadEntryInteractor(presenter, dataAccess);

        UUID userId = UUID.randomUUID();
        interactor.execute(new LoadEntryInputData(userId, "entries/1.json"));

        assertEquals(userId, dataAccess.requestedUserId);
        assertEquals("entries/1.json", dataAccess.requestedPath);
        assertTrue(dataAccess.getCalled);
        assertNotNull(presenter.successData);
        assertEquals(entry.getTitleCiphertext(), presenter.successData.getTitleCiphertext());
        assertEquals(entry.getBodyCiphertext(), presenter.successData.getBodyCiphertext());
        assertNull(presenter.errorMessage);
    }

    // Ensures validation fails fast when the entry path is missing.
    @Test
    void execute_withEmptyPath_reportsFailure() {
        RecordingLoadEntryPresenter presenter = new RecordingLoadEntryPresenter();
        StubLoadEntryDataAccess dataAccess = new StubLoadEntryDataAccess();
        LoadEntryInteractor interactor = new LoadEntryInteractor(presenter, dataAccess);

        interactor.execute(new LoadEntryInputData(UUID.randomUUID(), ""));

        assertEquals("Entry path cannot be empty.", presenter.errorMessage);
        assertNull(presenter.successData);
        assertFalse(dataAccess.getCalled);
    }

    // Ensures DAO exceptions are surfaced as a failure view.
    @Test
    void execute_whenDataAccessThrows_reportsFailure() {
        RecordingLoadEntryPresenter presenter = new RecordingLoadEntryPresenter();
        FailingLoadEntryDataAccess dataAccess = new FailingLoadEntryDataAccess();
        LoadEntryInteractor interactor = new LoadEntryInteractor(presenter, dataAccess);

        interactor.execute(new LoadEntryInputData(UUID.randomUUID(), "entries/2.json"));

        assertEquals("Failed to load entry: boom", presenter.errorMessage);
        assertNull(presenter.successData);
    }

    // Ensures a null entry from DAO is treated as a failure.
    @Test
    void execute_whenDaoReturnsNull_reportsFailure() {
        RecordingLoadEntryPresenter presenter = new RecordingLoadEntryPresenter();
        StubLoadEntryDataAccess dataAccess = new StubLoadEntryDataAccess();
        dataAccess.setEntryToReturn(null);
        LoadEntryInteractor interactor = new LoadEntryInteractor(presenter, dataAccess);

        interactor.execute(new LoadEntryInputData(UUID.randomUUID(), "entries/3.json"));

        assertEquals("Failed to load entry from path: entries/3.json", presenter.errorMessage);
        assertNull(presenter.successData);
    }

    @Test
    void loadEntryOutputData_exposesDate() {
        LocalDateTime now = LocalDateTime.now();
        LoadEntryOutputData data = new LoadEntryOutputData(
                "title",
                "iv",
                "body",
                "body-iv",
                "AES-GCM",
                1,
                "entries/1.json",
                now,
                now,
                true
        );
        assertEquals(now, data.getCreatedAt());
        assertEquals(now, data.getUpdatedAt());
    }

    private static final class RecordingLoadEntryPresenter implements LoadEntryOutputBoundary {
        private LoadEntryOutputData successData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(LoadEntryOutputData outputData) {
            this.successData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    private static class StubLoadEntryDataAccess implements LoadEntryUserDataAccessInterface {
        private DiaryEntry entryToReturn = new DiaryEntry();
        private boolean getCalled;
        private String requestedPath;
        private UUID requestedUserId;

        @Override
        public DiaryEntry getByPath(UUID userId, String entryPath) {
            this.getCalled = true;
            this.requestedUserId = userId;
            this.requestedPath = entryPath;
            return entryToReturn;
        }

        void setEntryToReturn(DiaryEntry entryToReturn) {
            this.entryToReturn = entryToReturn;
        }
    }

    private static class FailingLoadEntryDataAccess implements LoadEntryUserDataAccessInterface {
        @Override
        public DiaryEntry getByPath(UUID userId, String entryPath) {
            throw new RuntimeException("boom");
        }
    }
}

