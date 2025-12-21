package com.sentimentscribe.data;

import com.sentimentscribe.usecase.delete_entry.DeleteEntryUserDataAccessInterface;
import com.sentimentscribe.usecase.load_entry.LoadEntryUserDataAccessInterface;
import com.sentimentscribe.usecase.save_entry.SaveEntryUserDataAccessInterface;
import com.sentimentscribe.usecase.verify_password.RenderEntriesUserDataInterface;

public interface DiaryEntryRepository extends SaveEntryUserDataAccessInterface,
        LoadEntryUserDataAccessInterface,
        DeleteEntryUserDataAccessInterface,
        RenderEntriesUserDataInterface {
}
