package com.moodverse.adapter.new_document;

import com.moodverse.adapter.ViewModel;

/**
 * The ViewModel for the New Document View.
 */
public class NewDocumentViewModel extends ViewModel<NewDocumentState> {

    public NewDocumentViewModel() {
        super("new document");
        setState(new NewDocumentState());
    }
}

