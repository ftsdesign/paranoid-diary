package biz.ftsdesign.paranoiddiary;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import biz.ftsdesign.paranoiddiary.data.TransientPasswordStorage;

public class ChangePasswordDialogFragment extends DialogFragment {
    private Button positiveButton;
    private ChangePasswordDialogFragment.ChangePasswordDialogListener listener;

    public interface ChangePasswordDialogListener {
        void onChangePassword(String oldPassword, String newPassword);
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
        listener = (ChangePasswordDialogFragment.ChangePasswordDialogListener) context;
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.change_password_dialog_title);
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View layout = inflater.inflate(R.layout.dialog_change_password, null);
        final EditText oldPassword = layout.findViewById(R.id.EditText_OldPwd);
        final EditText password1 = layout.findViewById(R.id.EditText_Pwd1);
        final EditText password2 = layout.findViewById(R.id.EditText_Pwd2);
        final TextView error = layout.findViewById(R.id.TextView_PwdProblem);
        error.setText(R.string.password_cant_be_empty);

        TextWatcher textWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String strOldPass = oldPassword.getText().toString();
                String strPass1 = password1.getText().toString();
                String strPass2 = password2.getText().toString();
                if (!TransientPasswordStorage.isPasswordCorrect(strOldPass)) {
                    error.setText(R.string.incorrect_old_password);
                    if (positiveButton != null)
                        positiveButton.setEnabled(false);

                } else if (strPass1.isEmpty()) {
                    error.setText(R.string.password_cant_be_empty);
                    if (positiveButton != null)
                        positiveButton.setEnabled(false);

                } else {
                    if (strPass1.equals(strOldPass)) {
                        error.setText(R.string.new_pass_must_be_different);
                        if (positiveButton != null)
                            positiveButton.setEnabled(false);
                    } else if (strPass1.equals(strPass2)) {
                        error.setText("");
                        if (positiveButton != null)
                            positiveButton.setEnabled(true);
                    } else {
                        error.setText(R.string.new_passwords_dont_match);
                        if (positiveButton != null)
                            positiveButton.setEnabled(false);
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        };
        oldPassword.addTextChangedListener(textWatcher);
        password1.addTextChangedListener(textWatcher);
        password2.addTextChangedListener(textWatcher);

        builder.setView(layout)
                .setPositiveButton(R.string.export, (dialog, id) -> {
                    if (listener != null) {
                        listener.onChangePassword(oldPassword.getText().toString(), password1.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> ChangePasswordDialogFragment.this.getDialog().cancel());
        return builder.create();
    }
}
