package biz.ftsdesign.paranoiddiary;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG_EXPORT_ZIP_DIALOG_FRAGMENT = "ExportZipDialogFragment";
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
                doBackup();
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
    }

    private void doChangePassword() {
        DialogFragment dialog = new ChangePasswordDialogFragment();
        dialog.show(settingsActivity.getSupportFragmentManager(), TAG_CHANGE_PASSWORD_DIALOG_FRAGMENT);
    }

    private void doBackup() {
        DialogFragment dialog = new ExportZipDialogFragment();
        dialog.show(settingsActivity.getSupportFragmentManager(), TAG_EXPORT_ZIP_DIALOG_FRAGMENT);
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
}
