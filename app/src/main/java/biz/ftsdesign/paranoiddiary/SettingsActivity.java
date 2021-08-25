package biz.ftsdesign.paranoiddiary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
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
    private DataStorageService dataStorageService;

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

    private void doExport(String password, DataUtils.BackupFormat backupFormat) {
        Log.i(this.getClass().getSimpleName(), "Exporting all data as " + backupFormat);
        try {
            String timestampSuffix = "_" + Formats.FILE_TIMESTAMP_FORMAT.format(new Date());
            List<Record> records = dataStorageService.getAllRecords(DataUtils.DEFAULT_DIARY_ID);
            final byte[] recordsData;
            String filename;
            switch (backupFormat) {
                case JSON:
                    recordsData = DataUtils.toJson(records).getBytes();
                    filename = "ParanoidDiary" + timestampSuffix + ".json";
                    break;
                case TEXT:
                default:
                    recordsData = DataUtils.getRecordsAsText(records).getBytes();
                    filename = "ParanoidDiary" + timestampSuffix + ".txt";
                    break;
            }
            byte[] encryptedZipData = createEncryptedZip(recordsData, filename, password);
            File cacheDir = getFilesDir();
            File f = new File(cacheDir, "ParanoidDiary" + timestampSuffix + ".zip");
            Log.i(this.getClass().getCanonicalName(), f.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(f);
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

            /*
            On minSdkVersion 19 we cannot be sure that the backup file was actually shared and saved successfully,
            so whenever the chooser activity was launched, we count it as a successful backup.
             */
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPreferences.edit().putLong(getString(R.string.pref_key_last_backup_time), System.currentTimeMillis()).apply();
            Log.i(this.getClass().getSimpleName(), "Backup time recorded");

        } catch (Exception e) {
            Log.e(this.getClass().getCanonicalName(), e.getMessage(), e);
        }
    }

    private byte[] createEncryptedZip(byte[] recordsData, String filename, String password) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos, password.toCharArray());
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
        zipParameters.setFileNameInZip(filename);
        zipParameters.setEntrySize(recordsData.length);
        zos.putNextEntry(zipParameters);
        zos.write(recordsData);
        zos.closeEntry();
        zos.close();
        return baos.toByteArray();
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

    public void doBackupRestore() {
        // TODO
    }
}