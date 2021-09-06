package biz.ftsdesign.paranoiddiary;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import biz.ftsdesign.paranoiddiary.data.DataUtils;

public class ExportZipDialogFragment extends DialogFragment {
    private Button positiveButton;
    private ExportZipDialogListener listener;

    public interface ExportZipDialogListener {
        void onExportZipPasswordSet(String password, DataUtils.BackupFormat backupFormat);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.set_password_for_zip_file);
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.dialog_export_zip, null);
        final EditText password1 = layout.findViewById(R.id.EditText_Pwd1);
        final EditText password2 = layout.findViewById(R.id.EditText_Pwd2);
        final TextView error = layout.findViewById(R.id.TextView_PwdProblem);
        final Spinner formatSpinner = layout.findViewById(R.id.spinner_format);
        error.setText(R.string.password_cant_be_empty);

        TextWatcher textWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String strPass1 = password1.getText().toString();
                String strPass2 = password2.getText().toString();
                if (strPass1.isEmpty()) {
                    error.setText(R.string.password_cant_be_empty);
                    if (positiveButton != null)
                        positiveButton.setEnabled(false);

                } else {
                    if (strPass1.equals(strPass2)) {
                        error.setText("");
                        if (positiveButton != null)
                            positiveButton.setEnabled(true);
                    } else {
                        error.setText(R.string.passwords_dont_match);
                        if (positiveButton != null)
                            positiveButton.setEnabled(false);
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        };
        password1.addTextChangedListener(textWatcher);
        password2.addTextChangedListener(textWatcher);

        builder.setView(layout)
                .setPositiveButton(R.string.export, (dialog, id) -> {
                    if (listener != null) {
                        String formatString = formatSpinner.getSelectedItem().toString();
                        DataUtils.BackupFormat format;
                        // Be very careful here, because formatString is localized, only JSON is spelled the same in all supported languages
                        if (formatString.equalsIgnoreCase(DataUtils.BackupFormat.JSON.name())) {
                            format = DataUtils.BackupFormat.JSON;
                        } else {
                            format = DataUtils.BackupFormat.TEXT;
                        }
                        Log.i(this.getClass().getSimpleName(), formatString);
                        listener.onExportZipPasswordSet(password1.getText().toString(), format);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> ExportZipDialogFragment.this.getDialog().cancel());
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (ExportZipDialogListener) context;
    }
}
