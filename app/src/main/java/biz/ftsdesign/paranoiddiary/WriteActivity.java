package biz.ftsdesign.paranoiddiary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import biz.ftsdesign.paranoiddiary.data.DataStorageService;
import biz.ftsdesign.paranoiddiary.data.DataUtils;
import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;

public class WriteActivity extends AppCompatActivity implements ModifyTagsListener {
    private static final long MIN_AUTOSAVE_INTERVAL_MS = 5000;
    private static final String TAG_TAG_DIALOG_FRAGMENT = "TagDialogFragment";
    private DataStorageService dataStorageService;
    private long recordId; // Set in onCreate
    private Record record = null;

    // Autosave
    private Timer timer = new Timer();
    private TimerTask saveTextOnUpdateTimerTask = null;
    private long lastSaved = 0;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DataStorageService.DataStorageServiceBinder binder = (DataStorageService.DataStorageServiceBinder) service;
            dataStorageService = binder.getService();
            initUiFromRecord(recordId);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataStorageService = null;
        }
    };

    private void initUiFromRecord(final long recordId) {
        if (dataStorageService != null) {
            record = dataStorageService.getRecord(recordId);
            if (record == null) {
                Util.toastError(this, "Record id=" + recordId + " not found");
                finish();

            } else {
                EditText editText = findViewById(R.id.textViewRecordText);
                if (editText != null && editText.getText() == null) { // Don't restore if there's text already
                    editText.setText(record.getText());
                }
                TextView textViewTimestamp = findViewById(R.id.textViewWriteTimestamp);
                if (textViewTimestamp != null) {
                    Date date = new Date(record.getTimeCreated());
                    textViewTimestamp.setText(Formats.TIMESTAMP_FORMAT.format(date));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_writing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.action_tag:
                TagsDialogFragment dialog = new TagsDialogFragment(dataStorageService,
                        Util.getTagSelectionState(record), null, this, this);
                dialog.show(getSupportFragmentManager(), TAG_TAG_DIALOG_FRAGMENT);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(this.getClass().getSimpleName(), "onCreate");
        // To prevent the activity content to show up in the app thumbnail
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle b = getIntent().getExtras();
        recordId = b != null ? b.getLong(MainActivity.KEY_RECORD_ID, -1) : -1;

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> onFinishedWriting());

        TextView textViewRecordText = findViewById(R.id.textViewRecordText);
        textViewRecordText.requestFocus();
        textViewRecordText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Does nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Does nothing
            }

            @Override
            public void afterTextChanged(Editable e) {
                WriteActivity.this.onTextChanged(e);
            }
        });
    }

    private void onTextChanged(@NonNull Editable editable) {
        updateRecordFromUI(editable, record);

        final long now = System.currentTimeMillis();
        final long timeSinceLastSavedMs = now - lastSaved;
        if (timeSinceLastSavedMs > MIN_AUTOSAVE_INTERVAL_MS) {
            // Enough time has passed since last save, can save immediately
            saveCurrentRecord(record);

        } else {
            // Schedule save later, if not already scheduled
            if (saveTextOnUpdateTimerTask == null) {
                saveTextOnUpdateTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        saveCurrentRecord(record);
                        saveTextOnUpdateTimerTask = null;
                    }
                };
                long delay = MIN_AUTOSAVE_INTERVAL_MS - timeSinceLastSavedMs;
                timer.schedule(saveTextOnUpdateTimerTask, delay);
            }
        }
    }

    @Override
    public void onBackPressed() {
        onFinishedWriting();
    }

    /**
     * When we finished writing on user action
     */
    private void onFinishedWriting() {
        Log.i(this.getClass().getSimpleName(), "onFinishedWriting");
        saveBeforeClose(record);
        // We are done with editing, the activity will terminate
        record = null; // This is to block saving the record again in onStop()
        finish();
    }

    /**
     * Final update, including processing the tags from record's text
     */
    private synchronized void saveBeforeClose(@Nullable final Record record) {
        if (record != null) {
            boolean saved;
            Log.i(this.getClass().getSimpleName(), "saveBeforeClose " + record.getId());

            EditText editText = findViewById(R.id.textViewRecordText);
            if (editText != null) {
                updateRecordFromUI(editText.getText(), record);
            }

            addRecordTagsFromText(record);
            saved = saveCurrentRecord(record);
            Util.toastLong(this, "Record #" + record.getId() + " " + (saved ? "saved" : "NOT SAVED"));
        }
    }

    /**
     * Add tags from record text ("#tagname"). This method creates a new tag, if necessary,
     * but does not affect record-tag mappings.
     */
    private void addRecordTagsFromText(@NonNull final Record record) {
        try {
            Set<String> tagNames = DataUtils.extractTags(record.getText());
            for (String tagName : tagNames) {
                Tag tag = dataStorageService.getOrCreateTagByName(tagName);
                record.getTags().add(tag);
            }
        } catch (Exception e) {
            Util.toastException(this, e);
        }
    }

    /**
     * This method only updates the text, timestamp and explicitly defined tags.
     * Can be called from onFinishedWriting or from autosave.
     */
    private synchronized boolean saveCurrentRecord(@Nullable final Record record) {
        boolean saved = false;
        if (dataStorageService != null && record != null) {
            try {
                if (record.hasText()) {
                    Log.i(this.getClass().getSimpleName(), "Saving text (" + record.getText().length() + ") for record #" + record.getId());
                    dataStorageService.updateRecordAndTags(record);
                    saved = true;

                } else {
                    String msg = "Record #" + record.getId() + " has no text, deleting";
                    Log.i(WriteActivity.class.getSimpleName(), msg);
                    dataStorageService.deleteRecordAndTagMappings(record.getId());
                }
                lastSaved = System.currentTimeMillis();

            } catch (GeneralSecurityException e) {
                Util.toastException(WriteActivity.this, e);
            }
        }
        return saved;
    }

    @Override
    protected void onStart() {
        Log.i(this.getClass().getSimpleName(), "onStart");
        super.onStart();
        Intent intent = new Intent(this, DataStorageService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        Log.i(this.getClass().getSimpleName(), "onStop");
        if (saveTextOnUpdateTimerTask != null) {
            saveTextOnUpdateTimerTask.cancel();
        }
        if (record != null) {
            /*
            This happens if the activity goes to background before user finished writing.
            Don't clear the record as we might restart and continue writing.
             */
            saveBeforeClose(record);
        }
        super.onStop();
        unbindService(connection);
    }

    /**
     * Populates record's text from what is currently entered. This does not save the change.
     */
    private void updateRecordFromUI(@NonNull Editable editable, @NonNull final Record record) {
        String newText = editable.toString().trim();
        record.setText(newText);
        record.setTimeUpdated(System.currentTimeMillis());
    }

    @Override
    public void onTagsSelectionChanged(@NonNull List<Long> tagsToSetIds, @NonNull List<Long> tagsToUnsetIds) {
        for (long tagToSet : tagsToSetIds) {
            Tag tag = dataStorageService.getTag(tagToSet);
            record.getTags().add(tag);
        }
        for (long tagToUnset : tagsToUnsetIds) {
            Tag tag = dataStorageService.getTag(tagToUnset);
            record.getTags().remove(tag);
        }
        FlexboxLayout tagsBox = findViewById(R.id.tagsBox);
        Util.setTagsBox(this, tagsBox, record, null);
    }
}
