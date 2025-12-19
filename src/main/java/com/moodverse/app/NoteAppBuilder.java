package com.moodverse.app;

import com.moodverse.data.DBNoteDataObject;
import com.moodverse.data.NLPKeywordExtractor;
import com.moodverse.data.RecommendationAPIAccessObject;
import com.moodverse.data.NLPAnalysisDataAccessObject;
import com.moodverse.data.VerifyPasswordDataAccessObject;
import com.moodverse.adapter.GoBackPresenter;
import com.moodverse.adapter.ViewManagerModel;
import com.moodverse.adapter.home_menu.DeleteEntryPresenter;
import com.moodverse.adapter.home_menu.HomeMenuController;
import com.moodverse.adapter.home_menu.HomeMenuPresenter;
import com.moodverse.adapter.home_menu.HomeMenuViewModel;
import com.moodverse.adapter.lock_screen.LockScreenController;
import com.moodverse.adapter.lock_screen.LockScreenPresenter;
import com.moodverse.adapter.lock_screen.LockScreenViewModel;
import com.moodverse.adapter.new_document.NewDocumentController;
import com.moodverse.adapter.new_document.NewDocumentPresenter;
import com.moodverse.adapter.new_document.NewDocumentViewModel;
import com.moodverse.adapter.recommendation_menu.RecommendationMenuController;
import com.moodverse.adapter.recommendation_menu.RecommendationMenuPresenter;
import com.moodverse.adapter.recommendation_menu.RecommendationMenuViewModel;
import com.moodverse.usecase.analyze_keywords.AnalyzeKeywordsInputBoundary;
import com.moodverse.usecase.analyze_keywords.AnalyzeKeywordsInteractor;
import com.moodverse.usecase.create_entry.CreateEntryInputBoundary;
import com.moodverse.usecase.create_entry.CreateEntryInteractor;
import com.moodverse.usecase.delete_entry.DeleteEntryInputBoundary;
import com.moodverse.usecase.delete_entry.DeleteEntryInteractor;
import com.moodverse.usecase.get_recommendations.GetRecommendationsInputBoundary;
import com.moodverse.usecase.get_recommendations.GetRecommendationsInteractor;
import com.moodverse.usecase.go_back.GoBackInputBoundary;
import com.moodverse.usecase.go_back.GoBackInteractor;
import com.moodverse.usecase.load_entry.LoadEntryInputBoundary;
import com.moodverse.usecase.load_entry.LoadEntryInteractor;
import com.moodverse.usecase.save_entry.SaveEntryInputBoundary;
import com.moodverse.usecase.save_entry.SaveEntryInteractor;
import com.moodverse.usecase.verify_password.VerifyPasswordInputBoundary;
import com.moodverse.usecase.verify_password.VerifyPasswordInteractor;
import com.moodverse.view.HomeMenuView;
import com.moodverse.view.LockscreenView;
import com.moodverse.view.NewDocumentView;
import com.moodverse.view.RecommendationView;
import com.moodverse.view.ViewManager;

import java.awt.*;
import javax.swing.*;

/**
 * Builder for the MoodVerse Note Application.
 */
public class NoteAppBuilder {
    private final JPanel cardPanel = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    private final ViewManagerModel viewManagerModel = new ViewManagerModel();

    // Data Access Objects
    private final NLPAnalysisDataAccessObject nlpAnalysisDataAccessObject =
            NLPAnalysisDataAccessObject.createWithDefaultPipeline();
    private final DBNoteDataObject noteDataAccess = new DBNoteDataObject();
    private final VerifyPasswordDataAccessObject passwordDataAccess = new VerifyPasswordDataAccessObject();
    private final RecommendationAPIAccessObject recommendationDataAccess =
            new RecommendationAPIAccessObject(nlpAnalysisDataAccessObject);

    // View Models
    private LockScreenViewModel lockScreenViewModel;
    private HomeMenuViewModel homeMenuViewModel;
    private HomeMenuPresenter homeMenuPresenter;
    private NewDocumentViewModel newDocumentViewModel;
    private NewDocumentPresenter newDocumentPresenter;
    private RecommendationMenuViewModel recommendationMenuViewModel;

    // Views
    private NewDocumentView newDocumentView;
    private RecommendationView recommendationView;

    public NoteAppBuilder() {
        cardPanel.setLayout(cardLayout);
    }

    /**
     * Adds the LockScreen view to the application.
     * @return this builder
     */
    public NoteAppBuilder addLockScreenView() {
        lockScreenViewModel = new LockScreenViewModel();
        return this;
    }

    /**
     * Adds the HomeMenu view to the application.
     * @return this builder
     */
    public NoteAppBuilder addHomeMenuView() {
        homeMenuViewModel = new HomeMenuViewModel();
        homeMenuPresenter = new HomeMenuPresenter(homeMenuViewModel);
        return this;
    }

    /**
     * Adds the NewDocument view to the application.
     * @return this builder
     */
    public NoteAppBuilder addNewDocumentView() {
        newDocumentViewModel = new NewDocumentViewModel();
        newDocumentView = new NewDocumentView(newDocumentViewModel);
        cardPanel.add(newDocumentView, newDocumentViewModel.getViewName());
        return this;
    }

    /**
     * Adds the Recommendation view to the application.
     * @return this builder
     */
    public NoteAppBuilder addRecommendationView() {
        recommendationMenuViewModel = new RecommendationMenuViewModel();
        return this;
    }

    /**
     * Adds the verify password use case (LockScreen).
     * @return this builder
     */
    public NoteAppBuilder addVerifyPasswordUseCase() {
        if (homeMenuViewModel == null) {
            addHomeMenuView();
        }
        else if (homeMenuPresenter == null) {
            homeMenuPresenter = new HomeMenuPresenter(homeMenuViewModel);
        }
        final LockScreenPresenter presenter = new LockScreenPresenter(
                lockScreenViewModel, viewManagerModel, homeMenuViewModel, homeMenuPresenter);
        final VerifyPasswordInputBoundary interactor = new VerifyPasswordInteractor(
                passwordDataAccess, presenter, noteDataAccess);
        final LockScreenController controller = new LockScreenController(interactor);

        final LockscreenView lockscreenView = new LockscreenView(lockScreenViewModel, controller);
        cardPanel.add(lockscreenView, lockScreenViewModel.getViewName());
        return this;
    }

    /**
     * Adds the home menu use cases (create entry, load entry, delete entry).
     * @return this builder
     */
    public NoteAppBuilder addHomeMenuUseCases() {
        if (homeMenuViewModel == null) {
            addHomeMenuView();
        }
        if (newDocumentViewModel == null) {
            addNewDocumentView();
        }
        if (newDocumentPresenter == null) {
            newDocumentPresenter = new NewDocumentPresenter(newDocumentViewModel, viewManagerModel);
        }
        // Create Entry Use Case
        final CreateEntryInputBoundary createEntryInteractor = new CreateEntryInteractor(newDocumentPresenter);

        // Load Entry Use Case
        final LoadEntryInputBoundary loadEntryInteractor = new LoadEntryInteractor(
                newDocumentPresenter, noteDataAccess);

        // Delete Entry Use Case
        final DeleteEntryPresenter deleteEntryPresenter = new DeleteEntryPresenter(homeMenuViewModel);
        final DeleteEntryInputBoundary deleteEntryInteractor = new DeleteEntryInteractor(
                deleteEntryPresenter,
                noteDataAccess
        );

        final HomeMenuController controller = new HomeMenuController(
                createEntryInteractor, loadEntryInteractor, deleteEntryInteractor);

        final HomeMenuView homeMenuView = new HomeMenuView(controller, homeMenuViewModel);
        cardPanel.add(homeMenuView, homeMenuViewModel.getViewName());
        return this;
    }

    /**
     * Adds the new document use cases (save entry, get recommendations, go back).
     * @return this builder
     */
    public NoteAppBuilder addNewDocumentUseCases() {
        if (recommendationMenuViewModel == null) {
            recommendationMenuViewModel = new RecommendationMenuViewModel();
        }
        if (homeMenuViewModel == null) {
            addHomeMenuView();
        }
        if (newDocumentViewModel == null) {
            addNewDocumentView();
        }
        if (newDocumentPresenter == null) {
            newDocumentPresenter = new NewDocumentPresenter(newDocumentViewModel, viewManagerModel);
        }
        // Save Entry Use Case
        final SaveEntryInputBoundary saveEntryInteractor = new SaveEntryInteractor(
                newDocumentPresenter,
                noteDataAccess,
                new NLPKeywordExtractor(nlpAnalysisDataAccessObject));

        // Get Recommendations Use Case (results go to recommendation menu)
        final RecommendationMenuPresenter recommendationPresenter = new RecommendationMenuPresenter(
                recommendationMenuViewModel, viewManagerModel);
        final GetRecommendationsInputBoundary getRecommendationsInteractor = new GetRecommendationsInteractor(
                recommendationDataAccess, recommendationPresenter);

        // Analyze Keywords Use Case (stays within new document view)
        final AnalyzeKeywordsInputBoundary analyzeKeywordsInteractor = new AnalyzeKeywordsInteractor(
                nlpAnalysisDataAccessObject, newDocumentPresenter);

        // Go Back Use Case (from NewDocument to HomeMenu)
        final GoBackPresenter goBackPresenter = new GoBackPresenter(
                viewManagerModel, homeMenuViewModel);
        final GoBackInputBoundary goBackInteractor = new GoBackInteractor(goBackPresenter);

        final NewDocumentController controller = new NewDocumentController(
                getRecommendationsInteractor, goBackInteractor, saveEntryInteractor, analyzeKeywordsInteractor);

        newDocumentView.setNewDocumentController(controller);
        return this;
    }

    /**
     * Adds the recommendation menu use case (go back from recommendations).
     * @return this builder
     */
    public NoteAppBuilder addRecommendationMenuUseCase() {
        if (recommendationMenuViewModel == null) {
            recommendationMenuViewModel = new RecommendationMenuViewModel();
        }
        // Go Back Use Case (from Recommendation to NewDocument)
        final GoBackPresenter goBackPresenter = new GoBackPresenter(
                viewManagerModel, newDocumentViewModel);
        final GoBackInputBoundary goBackInteractor = new GoBackInteractor(goBackPresenter);

        // Get Recommendations interactor and presenter for this view
        final RecommendationMenuPresenter recommendationPresenter = new RecommendationMenuPresenter(
                recommendationMenuViewModel, viewManagerModel);
        final GetRecommendationsInputBoundary getRecommendationsInteractor = new GetRecommendationsInteractor(
                recommendationDataAccess, recommendationPresenter);

        final RecommendationMenuController controller = new RecommendationMenuController(
                getRecommendationsInteractor, goBackInteractor);

        if (recommendationView == null) {
            try {
                recommendationView = new RecommendationView(
                        recommendationMenuViewModel,
                        controller,
                        recommendationMenuViewModel.getState()
                );
                cardPanel.add(recommendationView, recommendationMenuViewModel.getViewName());
            }
            catch (Exception error) {
                error.printStackTrace();
            }
        }
        else {
            // Guard against empty recommendationView.
            // This way we simply switch to the existing view when the back button is pressed rather than instantiating
            // a new one.
            recommendationView.setRecommendationController(controller);
        }
        return this;
    }

    /**
     * Builds the application.
     * @return the JFrame for the application
     */
    public JFrame build() {
        final JFrame application = new JFrame("MoodVerse - Mood-Based Diary & Recommendations");
        application.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        application.setSize(1350, 1100);

        application.add(cardPanel);

        // Create a ViewManager to handle view switching
        new ViewManager(cardPanel, cardLayout, viewManagerModel);

        if (homeMenuPresenter != null && homeMenuViewModel != null) {
            viewManagerModel.addPropertyChangeListener(new HomeMenuRefreshListener(
                    homeMenuViewModel.getViewName(),
                    noteDataAccess,
                    homeMenuPresenter));
        }

        // Set initial view to LockScreen
        viewManagerModel.setState(lockScreenViewModel.getViewName());
        viewManagerModel.firePropertyChanged();

        return application;
    }
}

