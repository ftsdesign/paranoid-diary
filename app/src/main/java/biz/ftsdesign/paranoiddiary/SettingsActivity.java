package biz.ftsdesign.paranoiddiary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import biz.ftsdesign.paranoiddiary.data.DataStorageService;
import biz.ftsdesign.paranoiddiary.data.DataUtils;
import biz.ftsdesign.paranoiddiary.geo.GeoUtils;
import biz.ftsdesign.paranoiddiary.model.Record;

import static biz.ftsdesign.paranoiddiary.data.DataUtils.MIME_ZIP;

public class SettingsActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        ExportZipDialogFragment.ExportZipDialogListener,
        ChangePasswordDialogFragment.ChangePasswordDialogListener {
    public static final String KEY_ACTION_ON_START = "actionOnStart";
    public static final int ACTION_BACKUP = 1;
    private static final String TAG_EXPORT_ZIP_DIALOG_FRAGMENT = "ExportZipDialogFragment";
    private static final String BACKUP_FILENAME_PREFIX = "ParanoidDiary";
    private DataStorageService dataStorageService;
    private ActivityResultLauncher<String> backupRestoreFileSelectedLauncher;
    private ActivityResultLauncher<String> saveZipLauncher;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DataStorageService.DataStorageServiceBinder binder = (DataStorageService.DataStorageServiceBinder) service;
            dataStorageService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataStorageService = null;
        }
    };

    private byte[] encryptedZipData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        backupRestoreFileSelectedLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), this::onBackupRestoreFileSelected);
        saveZipLauncher =
                registerForActivityResult(new ActivityResultContracts.CreateDocument(), this::onSaveZipLocationSelected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, DataStorageService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
        unbindService(connection);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_theme))) {
            Log.i(this.getClass().getCanonicalName(), "onSharedPreferenceChanged " + key);
            Util.setThemeGlobal(sharedPreferences.getString(key, "MODE_NIGHT_YES"));
        } else if (key.equals(getString(R.string.pref_key_geotagging_enabled))) {
            // Dummy call to trigger the permission dialog
            GeoUtils.getGeoTag(this);
        }
    }

    @Override
    public void onExportZipPasswordSet(String password, DataUtils.BackupFormat backupFormat) {
        doExport(password, backupFormat);
    }

    public DataStorageService getDataStorageService() {
        return dataStorageService;
    }

    private void doExport(@NonNull String password, @NonNull DataUtils.BackupFormat backupFormat) {
        Log.i(this.getClass().getSimpleName(), "Exporting all data as " + backupFormat);
        try {
            final String timestamp = "_" + Formats.FILE_TIMESTAMP_FORMAT.format(new Date());
            final String zipFileName = BACKUP_FILENAME_PREFIX + timestamp + ".zip";
            encryptedZipData = getEncryptedZipData(password, backupFormat, timestamp);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.what_do_you_want_to_do_with_backup));

            builder.setPositiveButton(getString(R.string.share), (dialog, which) -> {
                dialog.dismiss();
                doShareZip(zipFileName);
            });
            builder.setNegativeButton(getString(R.string.save), (dialog, which) -> {
                dialog.dismiss();
                doChooseSaveZipLocation(zipFileName);
            });
            builder.setNeutralButton(getString(R.string.cancel), (dialog, which) -> {
                dialog.cancel();
                encryptedZipData = null;
            });

            builder.show();

        } catch (Exception e) {
            Log.e(this.getClass().getCanonicalName(), e.getMessage(), e);
        }
    }

    private void recordBackupTime() {
        /*
        On minSdkVersion 19 we cannot be sure that the backup file was actually shared and saved successfully,
        so whenever the chooser activity was launched, we count it as a successful backup.
         */
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putLong(getString(R.string.pref_key_last_backup_time), System.currentTimeMillis()).apply();
        Log.i(this.getClass().getSimpleName(), "Backup time recorded");
    }

    @NonNull
    private byte[] getEncryptedZipData(@NonNull String password, @NonNull DataUtils.BackupFormat backupFormat,
                                       @NonNull String timestamp) throws GeneralSecurityException, IOException {
        List<Record> records = dataStorageService.getAllRecords(DataUtils.DEFAULT_DIARY_ID);
        final byte[] recordsData;
        final String filename;
        switch (backupFormat) {
            case JSON:
                recordsData = DataUtils.toJson(records).getBytes();
                filename = BACKUP_FILENAME_PREFIX + timestamp + ".json";
                break;
            case TEXT:
            default:
                recordsData = DataUtils.getRecordsAsText(records).getBytes();
                filename = BACKUP_FILENAME_PREFIX + timestamp + ".txt";
                break;
        }
        return DataUtils.createEncryptedZip(recordsData, filename, password);
    }

    private void doChooseSaveZipLocation(String zipFileName) {
        saveZipLauncher.launch(zipFileName);
    }

    private void onSaveZipLocationSelected(final Uri targetUri) {
        if (encryptedZipData == null) {
            Util.toastError(this, "Nothing to save");
            Log.w(this.getClass().getSimpleName(), "encryptedZipData became null before save");
            return;
        }

        try (OutputStream os = getContentResolver().openOutputStream(targetUri)) {
            os.write(encryptedZipData);
            recordBackupTime();

        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Cannot write file: " + e.getMessage(), e);
            Util.toastError(this, "Cannot write file: " + e.getMessage());

        } finally {
            encryptedZipData = null;
        }
    }

    private void doShareZip(@NonNull String zipFileName) {
        if (encryptedZipData == null) {
            Util.toastError(this, "Nothing to save");
            Log.w(this.getClass().getSimpleName(), "encryptedZipData became null before share");
            return;
        }
        File cacheDir = getFilesDir();
        File f = new File(cacheDir, zipFileName);
        Log.i(this.getClass().getCanonicalName(), f.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(encryptedZipData);
            fos.close();
            Uri uri = FileProvider.getUriForFile(this, "biz.ftsdesign.paranoiddiary.provider", f);

            Intent sendIntent = new Intent();
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.setType(MIME_ZIP);
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);

            recordBackupTime();

        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Cannot share backup file: " + e.getMessage(), e);
            Util.toastError(this, "Cannot share backup file: " + e.getMessage());

        } finally {
            encryptedZipData = null;
        }
    }

    @Override
    public void onChangePassword(String oldPassword, String newPassword) {
        try {
            dataStorageService.globalChangePassword(newPassword);
        } catch (GeneralSecurityException e) {
            Util.toastException(this, e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleActionOnStart();
    }

    /**
     * When we want the SettingsActivity not just open, but to start a specific action
     */
    private void handleActionOnStart() {
        Bundle b = getIntent().getExtras();
        if (b != null && b.containsKey(KEY_ACTION_ON_START)) {
            if (b.getInt(KEY_ACTION_ON_START) == ACTION_BACKUP) {
                doBackup();
            }
        }
    }

    void doBackup() {
        DialogFragment dialog = new ExportZipDialogFragment();
        dialog.show(getSupportFragmentManager(), TAG_EXPORT_ZIP_DIALOG_FRAGMENT);
    }

    void chooseBackupFileToRestore() {
        backupRestoreFileSelectedLauncher.launch(MIME_ZIP);
    }

    private void onBackupRestoreFileSelected(final Uri backupFileUri) {
        if (backupFileUri == null) // No file was selected
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.enter_backup_password));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.open_backup_file,
                (dialog, which) -> onBackupFileOpened(backupFileUri, input.getText().toString().trim().toCharArray()));
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void onBackupFileOpened(final Uri result, final char[] password) {
        if (result != null && password.length > 0) {
            Log.i(this.getClass().getSimpleName(), "Backup file selected: " + result);
            final List<Record> records = getRecordsFromBackupZip(result, password);
            if (!records.isEmpty()) {
                if (dataStorageService.getRecordsCount() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.choose_backup_restore_method));
                    builder.setMessage(getString(R.string.choose_backup_restore_method_msg, dataStorageService.getRecordsCount()));
                    builder.setPositiveButton(getString(R.string.backup_restore_method_replace),
                            (dialog, which) -> doRestoreFromBackup(records, DataStorageService.BackupRestoreMode.REPLACE));
                    builder.setNeutralButton(getString(R.string.backup_restore_method_add),
                            (dialog, which) -> doRestoreFromBackup(records, DataStorageService.BackupRestoreMode.ADD));
                    builder.show();

                } else {
                    doRestoreFromBackup(records, DataStorageService.BackupRestoreMode.ADD);
                }

            }
        }
    }

    private void doRestoreFromBackup(@NonNull List<Record> records, @NonNull DataStorageService.BackupRestoreMode restoreMode) {
        int restoredRecordsCount = dataStorageService.restoreBackup(records, restoreMode);
        Util.toastLong(this, "Restored " + restoredRecordsCount + " records");
    }

    @NonNull
    private List<Record> getRecordsFromBackupZip(@NonNull Uri zipFileUri, @NonNull char[] password) {
        List<Record> records = Collections.emptyList();
        try  {
            records = DataUtils.getRecordsFromBackupZip(getContentResolver().openInputStream(zipFileUri), password);
            Log.i(this.getClass().getSimpleName(), "Records loaded from backup file: " + records.size());

        } catch (IOException e) {
            Util.toastLong(this, "Can't open backup zip file: " + e.getMessage());
            Log.e(this.getClass().getSimpleName(), "Can't open backup zip file: " + e.getMessage(), e);
        }
        return records;
    }

}