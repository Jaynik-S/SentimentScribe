package com.moodverse.data;

import com.moodverse.usecase.delete_entry.DeleteEntryUserDataAccessInterface;
import com.moodverse.usecase.load_entry.LoadEntryUserDataAccessInterface;
import com.moodverse.usecase.save_entry.SaveEntryUserDataAccessInterface;
import com.moodverse.usecase.verify_password.RenderEntriesUserDataInterface;

public interface DiaryEntryRepository extends SaveEntryUserDataAccessInterface,
        LoadEntryUserDataAccessInterface,
        DeleteEntryUserDataAccessInterface,
        RenderEntriesUserDataInterface {
}
