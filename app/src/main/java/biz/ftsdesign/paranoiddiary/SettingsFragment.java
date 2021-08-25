package biz.ftsdesign.paranoiddiary;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import biz.ftsdesign.paranoiddiary.model.Record;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG_CHANGE_PASSWORD_DIALOG_FRAGMENT = "ChangePasswordDialogFragment";
    private SettingsActivity settingsActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        settingsActivity = (SettingsActivity) context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        Preference deleteAllDataButton = getPreferenceManager().findPreference(getString(R.string.pref_key_delete_all_data));
        if (deleteAllDataButton != null) {
            deleteAllDataButton.setOnPreferenceClickListener(preference -> {
                confirmDeleteAllData();
                return true;
            });
        }

        Preference backupButton = getPreferenceManager().findPreference(getString(R.string.pref_key_backup));
        if (backupButton != null) {
            backupButton.setOnPreferenceClickListener(preference -> {
                settingsActivity.doBackup();
                return true;
            });
        }

        Preference backupRestoreButton = getPreferenceManager().findPreference(getString(R.string.pref_key_backup_restore));
        if (backupRestoreButton != null) {
            backupRestoreButton.setOnPreferenceClickListener(preference -> {
                settingsActivity.chooseBackupFileToRestore();
                return true;
            });
        }

        Preference changePasswordButton = getPreferenceManager().findPreference(getString(R.string.pref_key_change_password));
        if (changePasswordButton != null) {
            changePasswordButton.setOnPreferenceClickListener(preference -> {
                doChangePassword();
                return true;
            });
        }

        Preference showDiaryInfoButton = getPreferenceManager().findPreference(getString(R.string.pref_key_show_diary_info));
        if (showDiaryInfoButton != null) {
            showDiaryInfoButton.setOnPreferenceClickListener(preference -> {
                doShowDiaryInfo();
                return true;
            });
        }
    }

    private void doChangePassword() {
        DialogFragment dialog = new ChangePasswordDialogFragment();
        dialog.show(settingsActivity.getSupportFragmentManager(), TAG_CHANGE_PASSWORD_DIALOG_FRAGMENT);
    }

    private void confirmDeleteAllData() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.confirm))
                .setMessage(getString(R.string.delete_all_confirm))
                .setIcon(R.drawable.ic_action_warning)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    Toast.makeText(SettingsFragment.this.getContext(), getString(R.string.deleting_all_data), Toast.LENGTH_SHORT).show();
                    ((SettingsActivity)getActivity()).getDataStorageService().deleteAllData();
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void doShowDiaryInfo() {
        long recordsCount = settingsActivity.getDataStorageService().getRecordsCount();
        String firstRecordTimestamp = "";
        String lastRecordTimestamp = "";
        if (recordsCount > 0) {
            Record firstRecord = settingsActivity.getDataStorageService().getFirstRecord();
            if (firstRecord != null) {
                firstRecordTimestamp = Formats.format(Formats.TIMESTAMP_FORMAT, firstRecord.getTimeCreated());
            }
            Record lastRecord = settingsActivity.getDataStorageService().getLastRecord();
            if (lastRecord != null) {
                lastRecordTimestamp = Formats.format(Formats.TIMESTAMP_FORMAT, lastRecord.getTimeCreated());
            }
        }
        int tagsCount = settingsActivity.getDataStorageService().getTagsCount();

        String message = getString(R.string.diary_info_message, recordsCount, firstRecordTimestamp, lastRecordTimestamp, tagsCount);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.diary_info))
                .setMessage(message)
                .setIcon(R.drawable.ic_action_info)
                .setPositiveButton(R.string.close, null).show();
    }
}
