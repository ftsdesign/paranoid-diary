package biz.ftsdesign.paranoiddiary;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import biz.ftsdesign.paranoiddiary.data.DataStorageService;
import biz.ftsdesign.paranoiddiary.model.Tag;

public class TagsDialogFragment extends DialogFragment {
    private final Map<Tag, MultiSelectionState> tagToSelectionState;
    private final String header;
    private final DataStorageService dataStorageService;
    private final ModifyTagsListener modifyTagsListener;
    private final Activity activity;
    private TagListAdapter tagListAdapter;
    private ListView listView;
    private Button buttonAddTag;

    TagsDialogFragment(@NonNull DataStorageService dataStorageService,
                       @NonNull Map<Tag, MultiSelectionState> tagToSelectionState,
                       @Nullable String header,
                       @NonNull ModifyTagsListener modifyTagsListener,
                       @NonNull Activity activity) {
        this.dataStorageService = dataStorageService;
        this.tagToSelectionState = tagToSelectionState;
        this.header = header;
        this.modifyTagsListener = modifyTagsListener;
        this.activity = activity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        tagListAdapter = new TagListAdapter(activity, dataStorageService);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.set_tags);
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.dialog_set_tags_for_record, null);
        buttonAddTag = layout.findViewById(R.id.buttonAddNewTag);
        enableAddTagButton(false);
        EditText editTextNewTagName = layout.findViewById(R.id.editTextNewTagName);

        buttonAddTag.setOnClickListener(v -> doAddTag(editTextNewTagName));

        editTextNewTagName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String tagName = editTextNewTagName.getText().toString().trim();
                boolean canAddTag = !tagName.isEmpty() && dataStorageService.getTagByName(tagName) == null;
                enableAddTagButton(canAddTag);
            }
        });

        TextView headerTextView = layout.findViewById(R.id.textViewRecordsAffected);
        if (header != null && !header.trim().isEmpty()) {
            headerTextView.setText(header);
        } else {
            headerTextView.setVisibility(View.GONE);
        }
        listView = layout.findViewById(R.id.listViewTagsList);

        listView.setAdapter(tagListAdapter);

        for (int i = 0; i < dataStorageService.getAllTagsSortedByName().size(); i++) {
            Tag tag = dataStorageService.getAllTagsSortedByName().get(i);
            MultiSelectionState state = tagToSelectionState.get(tag);
            if (state == null)
                state = MultiSelectionState.NONE;
            boolean checked = state == MultiSelectionState.SOME || state == MultiSelectionState.ALL;
            if (checked)
                listView.setItemChecked(i, checked);
        }

        builder.setView(layout)
                .setPositiveButton(R.string.done, (dialog, whichButton) -> onTagsSelectionDone())
                .setNegativeButton(R.string.cancel, (dialog, id) -> onCancel());
        return builder.create();
    }

    private void onTagsSelectionDone() {
        long[] checkedTagsArray = listView.getCheckedItemIds();
        final List<Long> tagsToSetIds = new ArrayList<>(checkedTagsArray.length);
        for (long id : checkedTagsArray) {
            tagsToSetIds.add(id);
        }

        final List<Long> tagsToUnsetIds = new LinkedList<>();
        for (Tag tag : dataStorageService.getAllTagsSortedByName()) {
            if (!tagsToSetIds.contains(tag.getId()))
                tagsToUnsetIds.add(tag.getId());
        }
        modifyTagsListener.onTagsSelectionChanged(tagsToSetIds, tagsToUnsetIds);
    }

    private void onCancel() {
        Dialog dialog = TagsDialogFragment.this.getDialog();
        if (dialog != null) {
            dialog.cancel();
        }
    }

    private void enableAddTagButton(boolean enabled) {
        buttonAddTag.setEnabled(enabled);
        buttonAddTag.setClickable(enabled);
    }

    private void doAddTag(@NonNull EditText editText) {
        try {
            String tagName = editText.getText().toString();
            Tag tag = dataStorageService.getOrCreateTagByName(tagName.trim());
            tagListAdapter.reload();
            editText.setText("");
            listView.setItemChecked(tagListAdapter.getPosition(tag), true);
        } catch (GeneralSecurityException e) {
            Util.toastException(getActivity(), e);
        }
    }
}
