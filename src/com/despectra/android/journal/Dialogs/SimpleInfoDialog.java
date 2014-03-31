package com.despectra.android.journal.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

/**
 * Created by Dmitry on 27.03.14.
 */
public class SimpleInfoDialog extends DialogFragment {
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TITLE = "title";

    private String mTitle;
    private String mMessage;

    public static SimpleInfoDialog newInstance(String title, String message) {
        SimpleInfoDialog dialog = new SimpleInfoDialog();
        Bundle args = new Bundle();
        args.putString(KEY_MESSAGE, message);
        args.putString(KEY_TITLE, title);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitle = getArguments().getString(KEY_TITLE);
        mMessage = getArguments().getString(KEY_MESSAGE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(mMessage)
                .setTitle(mTitle)
                .setPositiveButton("OK", null);
        return builder.create();
    }

    public void setMessage(String message) {
        mMessage = message;
        if (isVisible()) {
            ((AlertDialog) getDialog()).setMessage(mMessage);
        }
    }
}
