package biz.ftsdesign.paranoiddiary;

import androidx.annotation.NonNull;

import java.util.List;

interface ModifyTagsListener {
    void onTagsSelectionChanged(@NonNull List<Long> tagsToSetIds, @NonNull List<Long> tagsToUnsetIds);
}
