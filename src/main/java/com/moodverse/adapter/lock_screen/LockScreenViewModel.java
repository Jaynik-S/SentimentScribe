package com.moodverse.adapter.lock_screen;

import com.moodverse.adapter.ViewModel;

public class LockScreenViewModel extends ViewModel<LockScreenState> {
    public LockScreenViewModel() {
        super("lockscreen");
        setState(new LockScreenState());
    }
}


