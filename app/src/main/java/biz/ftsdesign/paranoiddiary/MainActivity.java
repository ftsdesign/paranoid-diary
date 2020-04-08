package biz.ftsdesign.paranoiddiary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.IBinder;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import biz.ftsdesign.paranoiddiary.data.DataStorageService;
import biz.ftsdesign.paranoiddiary.data.DataUtils;
import biz.ftsdesign.paranoiddiary.data.PasswordListener;
import biz.ftsdesign.paranoiddiary.data.TransientPasswordStorage;
import biz.ftsdesign.paranoiddiary.geo.GeoUtils;
import biz.ftsdesign.paranoiddiary.model.GeoTag;
import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;
import biz.ftsdesign.paranoiddiary.predicate.NamedPredicate;
import biz.ftsdesign.paranoiddiary.predicate.TagPredicate;

public class MainActivity extends AppCompatActivity
        implements PasswordListener, RecordPredicateListener, InitDialogFragment.InitDialogListener {
    public static final String KEY_RECORD_ID = "record.id";
    private static final String TAG_INIT_DIALOG_FRAGMENT = "InitDialogFragment";
    private static final String TAG_TAG_DIALOG_FRAGMENT = "TagDialogFragment";
    private DataStorageService dataStorageService;
    private RecordsViewAdapter recordsViewAdapter;
    private Menu actionBarMenu;
    private SelectionTracker<Long> selectionTracker;
    private int recordsCountBeforeWrite;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DataStorageService.DataStorageServiceBinder binder = (DataStorageService.DataStorageServiceBinder) service;
            dataStorageService = binder.getService();
            dataStorageService.cleanup();
            if (!TransientPasswordStorage.isSet()) {
                setControlsEnabled(false);
                if (dataStorageService.isPasswordSet()) {
                    getPasswordFromUser();
                } else {
                    Log.i(MainActivity.this.getClass().getSimpleName(), "First time setup");
                    InitDialogFragment dialog = new InitDialogFragment();
                    dialog.show(getSupportFragmentManager(), TAG_INIT_DIALOG_FRAGMENT);
                }
            }
            if (recordsViewAdapter != null) {
                recordsViewAdapter.setDataStorageService(dataStorageService);
            }
            loadAllRecordsNoText();

            onNewRecordAdded();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (recordsViewAdapter != null) {
                recordsViewAdapter.removeDataStorageService();
            }
            dataStorageService = null;
        }
    };

    @Override
    public void onAfterPasswordCleared() {
        runOnUiThread(() -> {
            setControlsEnabled(false);
            setToggleLockLocked();
            onRecordsUpdateFinished(Collections.emptyList());
        });
    }

    @Override
    public void onPasswordSet() {
        // TODO
    }

    private void loadAllRecordsNoText() {
        try {
            onRecordsUpdateFinished(dataStorageService.getAllRecordsNoText(DataUtils.DEFAULT_DIARY_ID));
        } catch (GeneralSecurityException e) {
            Util.toastException(MainActivity.this, e);
        }
    }

    private void onPasswordUnlocked() {
        Log.i(this.getClass().getSimpleName(), "onPasswordUnlocked");
        setToggleLockUnlocked();
        setControlsEnabled(true);
        try {
            loadAllRecordsNoText();
        } catch (Exception e) {
            Util.toastException(MainActivity.this, e);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        setMenuItemEnabled(R.id.action_filter, enabled);
        setMenuItemEnabled(R.id.action_tag, enabled);
        setMenuItemEnabled(R.id.action_share, enabled);
        setMenuItemEnabled(R.id.action_delete, enabled);
        setMenuItemEnabled(R.id.action_settings, enabled);
        setFloatingActionButtonEnabled(enabled);
    }

    private void setMenuItemEnabled(int itemId, boolean enabled) {
        if (actionBarMenu != null) {
            MenuItem menuItem = actionBarMenu.findItem(itemId);
            if (menuItem != null) {
                menuItem.setEnabled(enabled);
            }
        }
    }

    private void getPasswordFromUser() {
        if (!TransientPasswordStorage.isSet()) {
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.enter_password))
                    .setIcon(R.drawable.ic_action_lock)
                    .setView(input)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        if (TransientPasswordStorage.setPassword(input.getText().toString())) {
                            if (!dataStorageService.isPasswordCorrect()) {
                                TransientPasswordStorage.clear();
                                Toast toast = Toast.makeText(MainActivity.this, getString(R.string.incorrect_password), Toast.LENGTH_LONG);
                                toast.show();
                                getPasswordFromUser();
                            } else {
                                onPasswordUnlocked();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        }
    }

    private void onRecordsUpdateFinished(@NonNull final List<Record> records) {
        Log.i(this.getClass().getSimpleName(), "onRecordsUpdateFinished " + records.size());
        recordsViewAdapter.setRecords(records);
        onRecordFilterUpdated();
    }

    @Override
    protected void onResume() {
        Log.i(this.getClass().getSimpleName(), "onResume");
        super.onResume();
    }

    @Override
    protected void onStart() {
        Log.i(this.getClass().getSimpleName(), "onStart");
        super.onStart();

        TransientPasswordStorage.addListener(this);
        Intent intent = new Intent(this, DataStorageService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void onNewRecordAdded() {
        Log.i(this.getClass().getSimpleName(), "onNewRecordAdded");
        if (recordsCountBeforeWrite > 0 && recordsViewAdapter.getItemCountUnfiltered() > recordsCountBeforeWrite) {
            Log.i(this.getClass().getSimpleName(), "New records: " + recordsViewAdapter.getItemCountUnfiltered() + " > " + recordsCountBeforeWrite);
            RecyclerView recyclerView = findViewById(R.id.recyclerViewRecords);
            if (recyclerView != null) {
                recordsViewAdapter.onNewRecordAdded();
                recyclerView.scrollToPosition(0);
            }
        }
    }

    @Override
    protected void onStop() {
        Log.i(this.getClass().getSimpleName(), "onStop");
        super.onStop();
        unbindService(connection);
        TransientPasswordStorage.removeListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(this.getClass().getSimpleName(), "onCreate");
        // To prevent the activity content to show up in the app thumbnail
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);
        /*
        HACK
        Because app name and main activity name are the same in AndroidManifest.xml,
        but we want the app to be "Paranoid Diary" and the activity "PD".
        https://stackoverflow.com/questions/2444040/naming-my-application-in-android
         */
        setTitle(R.string.title_activity_main);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Util.setThemeGlobal(sharedPreferences.getString(getString(R.string.pref_theme), "MODE_NIGHT_YES"));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        setFloatingActionButtonEnabled(TransientPasswordStorage.isSet());
        fab.setOnClickListener(view -> {
            try {
                onWrite();
            } catch (GeneralSecurityException e) {
                Util.toastException(MainActivity.this, e);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerViewRecords);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recordsViewAdapter = new RecordsViewAdapter(this, Collections.emptyList());
        recordsViewAdapter.setTagColor(getResources().getColor(R.color.tag));
        recyclerView.setAdapter(recordsViewAdapter);

        /*
        We only allow single selection here. Reasons:
        - There is no good way, from UX perspective, to deal with multiple selected items, spread across a very long list
        - Generally, we want user to work only with what he sees now, to make the interaction more intuitive
         */
        SelectionTracker.SelectionPredicate<Long> selectionPredicate = SelectionPredicates.createSelectSingleAnything();
        selectionTracker = new SelectionTracker.Builder<>(
                "my-selection-id",
                recyclerView,
                new CustomKeyProvider(recyclerView, recordsViewAdapter),
                new RecordsViewAdapter.RecordDetailsLookup(recyclerView),
                StorageStrategy.createLongStorage()).withSelectionPredicate(selectionPredicate)
                .build();
        recordsViewAdapter.setSelectionTracker(selectionTracker);

        GeoUtils.requestLocationUpdates(this);
    }

    private void onWrite() throws GeneralSecurityException {
        recordsCountBeforeWrite = recordsViewAdapter.getItemCountUnfiltered();
        Record record = new Record(-1);
        record.setDiaryId(DataUtils.DEFAULT_DIARY_ID);
        record.setGeoTag(GeoUtils.getGeoTag(this));
        record = dataStorageService.createNewRecord(record);
        Intent intent = new Intent(MainActivity.this, WriteActivity.class);
        intent.putExtra(KEY_RECORD_ID, record.getId());
        startActivity(intent);
    }

    private void setFloatingActionButtonEnabled(boolean enabled) {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setEnabled(enabled);
        if (enabled) {
            fab.show();
        } else {
            fab.hide();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        actionBarMenu = menu;
        setControlsEnabled(TransientPasswordStorage.isSet());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                return true;

            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;

            case R.id.action_tag:
                doTagRecords();
                return true;

            case R.id.action_clear_filter:
                doClearFilter();
                return true;

            case R.id.action_filter_tags:
                doSetFilterTags();
                return true;

            case R.id.action_share:
                if (selectionTracker.hasSelection()) {
                    doShare();
                } else {
                    toastSelectSomething();
                }
                return true;

            case R.id.action_delete:
                if (selectionTracker.hasSelection()) {
                    doDelete();
                } else {
                    toastSelectSomething();
                }
                return true;

            case R.id.action_toggle_lock:
                if (TransientPasswordStorage.isSet()) {
                    Log.i(this.getClass().getSimpleName(), "Locked on user action");
                    TransientPasswordStorage.clear();
                } else {
                    getPasswordFromUser();
                }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void doSetFilterTags() {
        TagsDialogFragment dialog = new TagsDialogFragment(dataStorageService, new HashMap<>(), "Set filter tag",
                new ModifyTagsListener() {
                    @Override
                    public void onTagsSelectionChanged(@NonNull List<Long> tagsToSetIds, @NonNull List<Long> tagsToUnsetIds) {
                        List<Tag> tags = dataStorageService.getTags(tagsToSetIds);
                        if (tags.isEmpty()) {
                            recordsViewAdapter.clearPredicate();
                        } else {
                            recordsViewAdapter.setPredicate(new TagPredicate(tags));
                        }
                    }
        }, this);
        dialog.show(getSupportFragmentManager(), TAG_TAG_DIALOG_FRAGMENT);
    }

    private void doTagRecords() {
        if (selectionTracker.hasSelection()) {
            Map<Tag, MultiSelectionState> tagToSelectionState = getTagsSelectionState(getSelectedRecords());
            final List<Long> selectedRecordIds = getSelectedRecordIds();
            String header = getString(R.string.records_affected, selectedRecordIds.size());
            TagsDialogFragment dialog = new TagsDialogFragment(dataStorageService, tagToSelectionState, header,
                    new ModifyTagsListener() {
                        @Override
                        public void onTagsSelectionChanged(@NonNull List<Long> tagsToSetIds, @NonNull List<Long> tagsToUnsetIds) {
                            final List<Long> selectedRecordIds = getSelectedRecordIds();
                            Log.i(this.getClass().getSimpleName(), "For " + selectedRecordIds.size() + " selected records, set " + tagsToSetIds.size() + ", unset " + tagsToUnsetIds.size() + " tags");
                            dataStorageService.bulkModifyTags(selectedRecordIds, tagsToSetIds, tagsToUnsetIds);
                            recordsViewAdapter.updateRecords(selectedRecordIds);
                        }
                    }, this);
            dialog.show(getSupportFragmentManager(), TAG_TAG_DIALOG_FRAGMENT);
        } else {
            toastSelectSomething();
        }
    }

    public void onRecordFilterUpdated() {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateActionFilterText(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    private void updateActionFilterText(Menu menu) {
        MenuItem actionFilterItem = menu.findItem(R.id.action_filter);
        int filteredRecordsCount = recordsViewAdapter.getItemCount();
        int totalRecordsCount = recordsViewAdapter.getItemCountUnfiltered();
        String text = filteredRecordsCount + "/" + totalRecordsCount;
        if (filteredRecordsCount < totalRecordsCount) {
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorAccent)), 0, text.length(), 0);
            actionFilterItem.setTitle(spannableString);
        } else {
            actionFilterItem.setTitle(text);
        }

        NamedPredicate<Record> predicate = recordsViewAdapter.getRecordPredicate();
        final String predicateText = predicate != null ? predicate.toString() : "";
        MenuItem tagFilterItem = menu.findItem(R.id.action_filter_tags);
        tagFilterItem.setTitle(predicateText);
    }

    @NonNull
    private Map<Tag, MultiSelectionState> getTagsSelectionState(final @NonNull List<Record> records) {
        final Map<Tag,Integer> tagToCount = new HashMap<>();
        for (Record record : records) {
            for (Tag tag : record.getTags()) {
                Integer count = tagToCount.get(tag);
                if (count == null) {
                    count = 0;
                }
                tagToCount.put(tag, ++count);
            }
        }

        final Map<Tag, MultiSelectionState> out = new HashMap<>();
        for (Map.Entry<Tag,Integer> entry : tagToCount.entrySet()) {
            final MultiSelectionState state;
            Integer count = entry.getValue();
            if (count == null || count == 0) {
                state = MultiSelectionState.NONE;
            } else if (count < records.size()) {
                state = MultiSelectionState.SOME;
            } else {
                state = MultiSelectionState.ALL;
            }
            out.put(entry.getKey(), state);
        }
        return out;
    }

    @NonNull
    private List<Long> getSelectedRecordIds() {
        final List<Long> recordIds = new ArrayList<>(selectionTracker.getSelection().size());
        for (long recordId : selectionTracker.getSelection()) {
            recordIds.add(recordId);
        }
        return recordIds;
    }

    private void doClearFilter() {
        recordsViewAdapter.clearPredicate();
    }

    private void toastSelectSomething() {
        Toast.makeText(this, "Select some records first", Toast.LENGTH_LONG).show();
    }

    private void setToggleLockLocked() {
        MenuItem menuItem = actionBarMenu.findItem(R.id.action_toggle_lock);
        if (menuItem != null) {
            menuItem.setIcon(R.drawable.ic_action_key);
            menuItem.setTitle(R.string.unlock);
        }
    }

    private void setToggleLockUnlocked() {
        MenuItem menuItem = actionBarMenu.findItem(R.id.action_toggle_lock);
        if (menuItem != null) {
            menuItem.setIcon(R.drawable.ic_action_locked);
            menuItem.setTitle(R.string.lock);
        }
    }

    private void doShare() {
        List<Record> recordsToShare = getSelectedRecords();
        if (!recordsToShare.isEmpty()) {
            String textToShare = getRecordsAsText(recordsToShare);

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        }
    }

    private @NonNull List<Record> getSelectedRecords() {
        List<Record> records = new LinkedList<>();
        for (long id : selectionTracker.getSelection()) {
            Record record = recordsViewAdapter.getRecordById(id);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private void doDelete() {
        final List<Record> recordsToDelete = getSelectedRecords();
        if (!recordsToDelete.isEmpty()) {
            String message;
            if (recordsToDelete.size() == 1) {
                final Record record = recordsToDelete.get(0);
                message = getString(R.string.are_you_sure_delete_record,
                        Util.composeRecordHeader(record),
                        Util.cutString(record.getText(), 20));
            } else {
                message = "Are you sure you want to permanently delete " + recordsToDelete.size() + " selected records?";
            }
            Log.i(this.getClass().getSimpleName(), "Message " + message);

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirm))
                    .setIcon(R.drawable.ic_action_warning)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        dataStorageService.deleteRecords(recordsToDelete);
                        recordsViewAdapter.deleteRecords(recordsToDelete);
                        selectionTracker.clearSelection();
                        onRecordFilterUpdated();
                        Toast.makeText(MainActivity.this, getString(R.string.toast_records_deleted, recordsToDelete.size()), Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        }
    }

    private String getRecordsAsText(List<Record> records) {
        ensureTextPopulated(records);
        return DataUtils.getRecordsAsText(records);
    }

    private void ensureTextPopulated(List<Record> records) {
        for (Record record : records) {
            if (!record.hasText()) {
                record.setText(dataStorageService.getRecordText(record.getId()));
            }
        }
    }

    public void shareGeoTag(GeoTag geoTag) {
        if (geoTag != null) {
            Uri gmmIntentUri = Uri.parse("geo:" + geoTag.getLat() + "," + geoTag.getLon() + "?q=" + geoTag.getLat()
                    + "," + geoTag.getLon());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            }
        }
    }

    @Override
    public void setRecordPredicate(NamedPredicate<Record> recordPredicate) {
        recordsViewAdapter.setPredicate(recordPredicate);
    }

    @Override
    public void onSetInitPassword(String password) {
        if (TransientPasswordStorage.setPassword(password)) {
            dataStorageService.savePwdCheck();
            onPasswordUnlocked();
        }
    }
}
