package com.sentimentscribe.usecase.save_entry;

import com.sentimentscribe.domain.DiaryEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SaveEntryInteractorTest {

    private static final String TITLE_CIPHERTEXT = "VGVzdCBUaXRsZQ==";
    private static final String BODY_CIPHERTEXT = "VGVzdCBCb2R5";
    private static final String IV = "AAAAAAAAAAAAAAAAAAAAAA==";

    @Test
    void execute_withValidInput_savesEntryAndReturnsSuccess() {
        RecordingSaveEntryPresenter presenter = new RecordingSaveEntryPresenter();
        InMemorySaveEntryDataAccess dataAccess = new InMemorySaveEntryDataAccess();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, dataAccess);

        UUID userId = UUID.randomUUID();
        SaveEntryInputData inputData = new SaveEntryInputData(
                userId,
                TITLE_CIPHERTEXT,
                IV,
                BODY_CIPHERTEXT,
                IV,
                "AES-GCM",
                1,
                null,
                LocalDateTime.now()
        );

        interactor.execute(inputData);

        assertTrue(dataAccess.saveCalled);
        assertEquals(userId, dataAccess.savedUserId);
        assertNotNull(dataAccess.savedEntry);
        assertNotNull(presenter.successData);
        assertNull(presenter.errorMessage);
        assertEquals(TITLE_CIPHERTEXT, presenter.successData.getTitleCiphertext());
        assertEquals(BODY_CIPHERTEXT, presenter.successData.getBodyCiphertext());
    }

    @Test
    void execute_withMissingTitleCiphertext_reportsFailure() {
        RecordingSaveEntryPresenter presenter = new RecordingSaveEntryPresenter();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, new InMemorySaveEntryDataAccess());

        SaveEntryInputData inputData = new SaveEntryInputData(
                UUID.randomUUID(),
                "",
                IV,
                BODY_CIPHERTEXT,
                IV,
                "AES-GCM",
                1,
                null,
                LocalDateTime.now()
        );

        interactor.execute(inputData);

        assertEquals("Title ciphertext is required.", presenter.errorMessage);
        assertNull(presenter.successData);
    }

    @Test
    void execute_withMissingTitleIv_reportsFailure() {
        RecordingSaveEntryPresenter presenter = new RecordingSaveEntryPresenter();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, new InMemorySaveEntryDataAccess());

        SaveEntryInputData inputData = new SaveEntryInputData(
                UUID.randomUUID(),
                TITLE_CIPHERTEXT,
                " ",
                BODY_CIPHERTEXT,
                IV,
                "AES-GCM",
                1,
                null,
                LocalDateTime.now()
        );

        interactor.execute(inputData);

        assertEquals("Title IV is required.", presenter.errorMessage);
        assertNull(presenter.successData);
    }

    @Test
    void execute_withMissingBodyCiphertext_reportsFailure() {
        RecordingSaveEntryPresenter presenter = new RecordingSaveEntryPresenter();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, new InMemorySaveEntryDataAccess());

        SaveEntryInputData inputData = new SaveEntryInputData(
                UUID.randomUUID(),
                TITLE_CIPHERTEXT,
                IV,
                null,
                IV,
                "AES-GCM",
                1,
                null,
                LocalDateTime.now()
        );

        interactor.execute(inputData);

        assertEquals("Body ciphertext is required.", presenter.errorMessage);
        assertNull(presenter.successData);
    }

    @Test
    void execute_withMissingBodyIv_reportsFailure() {
        RecordingSaveEntryPresenter presenter = new RecordingSaveEntryPresenter();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, new InMemorySaveEntryDataAccess());

        SaveEntryInputData inputData = new SaveEntryInputData(
                UUID.randomUUID(),
                TITLE_CIPHERTEXT,
                IV,
                BODY_CIPHERTEXT,
                "",
                "AES-GCM",
                1,
                null,
                LocalDateTime.now()
        );

        interactor.execute(inputData);

        assertEquals("Body IV is required.", presenter.errorMessage);
        assertNull(presenter.successData);
    }

    @Test
    void execute_withMissingAlgorithm_reportsFailure() {
        RecordingSaveEntryPresenter presenter = new RecordingSaveEntryPresenter();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, new InMemorySaveEntryDataAccess());

        SaveEntryInputData inputData = new SaveEntryInputData(
                UUID.randomUUID(),
                TITLE_CIPHERTEXT,
                IV,
                BODY_CIPHERTEXT,
                IV,
                "",
                1,
                null,
                LocalDateTime.now()
        );

        interactor.execute(inputData);

        assertEquals("Algorithm is required.", presenter.errorMessage);
        assertNull(presenter.successData);
    }

    @Test
    void execute_withInvalidVersion_reportsFailure() {
        RecordingSaveEntryPresenter presenter = new RecordingSaveEntryPresenter();
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, new InMemorySaveEntryDataAccess());

        SaveEntryInputData inputData = new SaveEntryInputData(
                UUID.randomUUID(),
                TITLE_CIPHERTEXT,
                IV,
                BODY_CIPHERTEXT,
                IV,
                "AES-GCM",
                0,
                null,
                LocalDateTime.now()
        );

        interactor.execute(inputData);

        assertEquals("Version must be positive.", presenter.errorMessage);
        assertNull(presenter.successData);
    }

    @Test
    void saveEntryOutputData_exposesCreatedAt() {
        LocalDateTime now = LocalDateTime.now();
        SaveEntryOutputData data = new SaveEntryOutputData(
                TITLE_CIPHERTEXT,
                IV,
                BODY_CIPHERTEXT,
                IV,
                "AES-GCM",
                1,
                null,
                now,
                now,
                true
        );
        assertEquals(now, data.getCreatedAt());
        assertEquals(now, data.getUpdatedAt());
    }

    @Test
    void execute_whenSaveThrowsException_reportsFailureWithMessage() {
        RecordingSaveEntryPresenter presenter = new RecordingSaveEntryPresenter();
        SaveEntryUserDataAccessInterface dataAccess = (userId, entry) -> { throw new RuntimeException("disk full"); };
        SaveEntryInteractor interactor = new SaveEntryInteractor(presenter, dataAccess);

        SaveEntryInputData inputData = new SaveEntryInputData(
                UUID.randomUUID(),
                TITLE_CIPHERTEXT,
                IV,
                BODY_CIPHERTEXT,
                IV,
                "AES-GCM",
                1,
                null,
                LocalDateTime.now()
        );

        interactor.execute(inputData);

        assertNotNull(presenter.errorMessage);
        assertTrue(presenter.errorMessage.startsWith("Could not save entry."));
        assertTrue(presenter.errorMessage.contains("disk full"));
        assertNull(presenter.successData);
    }

    private static final class RecordingSaveEntryPresenter implements SaveEntryOutputBoundary {
        private SaveEntryOutputData successData;
        private String errorMessage;

        @Override
        public void prepareSuccessView(SaveEntryOutputData outputData) {
            this.successData = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    private static final class InMemorySaveEntryDataAccess implements SaveEntryUserDataAccessInterface {
        private boolean saveCalled;
        private DiaryEntry savedEntry;
        private UUID savedUserId;

        @Override
        public boolean save(UUID userId, DiaryEntry entry) {
            this.saveCalled = true;
            this.savedEntry = entry;
            this.savedUserId = userId;
            return true;
        }
    }
}
