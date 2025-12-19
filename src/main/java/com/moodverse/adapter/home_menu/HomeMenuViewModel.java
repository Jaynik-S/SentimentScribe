package com.moodverse.adapter.home_menu;

import com.moodverse.adapter.ViewModel;

/**
 * View model for the home menu view.
 * This class holds the HomeMenuState used by the home menu UI and
 * notifies its observers when the state changes. Presenters update this
 * view model, and views listen for property changes to refresh the screen.
 */

public class HomeMenuViewModel extends ViewModel<HomeMenuState> {

    public static final String TITLE = "MoodVerse";

    public HomeMenuViewModel() {
        super("HomeMenu");
        setState(new HomeMenuState());
    }
}

