package biz.ftsdesign.paranoiddiary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import biz.ftsdesign.paranoiddiary.data.DataStorageService;
import biz.ftsdesign.paranoiddiary.data.DataUtils;
import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;

public class WriteActivity extends AppCompatActivity implements ModifyTagsListener {
    private static final long MIN_AUTOSAVE_INTERVAL_MS = 500;
    private static final String TAG_TAG_DIALOG_FRAGMENT = "TagDialogFragment";
    private long lastSaved = 0;
    private DataStorageService dataStorageService;
    private long recordId;
    private Record record;
    private Timer timer = new Timer();
    private TimerTask saveTextOnUpdateTimerTask = null;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DataStorageService.DataStorageServiceBinder binder = (DataStorageService.DataStorageServiceBinder) service;
            dataStorageService = binder.getService();
            initRecord();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataStorageService = null;
        }
    };

    private void initRecord() {
        if (dataStorageService != null) {
            record = dataStorageService.getRecord(recordId);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_writing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tag:
                TagsDialogFragment dialog = new TagsDialogFragment(dataStorageService, Util.getTagSelectionState(record), null);
                dialog.show(getSupportFragmentManager(), TAG_TAG_DIALOG_FRAGMENT);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(this.getClass().getCanonicalName(), "onCreate");
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

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                WriteActivity.this.onTextChanged();
            }
        });
    }

    private void onTextChanged() {
        updateRecordFromUI();

        final long now = System.currentTimeMillis();
        final long timeSinceLastSavedMs = now - lastSaved;
        if (timeSinceLastSavedMs > MIN_AUTOSAVE_INTERVAL_MS) {
            // Enough time has passed since last save, can save immediately
            saveCurrentRecord();

        } else {
            // Schedule save later, if not already scheduled
            if (saveTextOnUpdateTimerTask == null) {
                saveTextOnUpdateTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        saveCurrentRecord();
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
        super.onBackPressed();
    }

    private void onFinishedWriting() {
        Log.i(this.getClass().getSimpleName(), "onFinishedWriting");
        if (saveTextOnUpdateTimerTask != null) {
            saveTextOnUpdateTimerTask.cancel();
        }
        updateRecordFromUI();
        saveCurrentRecord();
        saveAutoTagsFromText(record);
        record = null;
        finish();
    }

    private void saveAutoTagsFromText(final Record record) {
        if (record != null) {
            try {
                Collection<String> tagNames = DataUtils.extractTags(record.getText());
                for (String tagName : tagNames) {
                    Tag tag = dataStorageService.getOrCreateTagByName(tagName);
                    record.getTags().add(tag);
                    dataStorageService.updateTags(record);
                }
            } catch (Exception e) {
                Util.toastException(this, e);
            }
        }
    }

    private void saveCurrentRecord() {
        if (dataStorageService != null && record != null) {
            try {
                Log.i(WriteActivity.class.getCanonicalName(), "" + record.getText().length());
                if (record.hasText()) {
                    Log.i(this.getClass().getCanonicalName(), "saving text " + record.getText().length());
                    dataStorageService.update(record);
                } else {
                    dataStorageService.delete(record.getId());
                }
                lastSaved = System.currentTimeMillis();
            } catch (GeneralSecurityException e) {
                Util.toastException(WriteActivity.this, e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(this.getClass().getCanonicalName(), "onDestroy");
        onFinishedWriting();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Log.i(this.getClass().getCanonicalName(), "onStart");
        super.onStart();
        Intent intent = new Intent(this, DataStorageService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        Log.i(this.getClass().getCanonicalName(), "onStop");
        saveCurrentRecord();
        super.onStop();
        unbindService(connection);
    }

    private void updateRecordFromUI() {
        if (record != null) {
            EditText editText = findViewById(R.id.textViewRecordText);
            if (editText != null) {
                String newText = editText.getText().toString().trim();
                if (!newText.equals(record.getText())) {
                    record.setText(newText);
                    record.setTimeUpdated(System.currentTimeMillis());
                }
            }
        }
    }

    @Override
    public void modifyTags(List<Long> tagsToSetIds, List<Long> tagsToUnsetIds) {
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
