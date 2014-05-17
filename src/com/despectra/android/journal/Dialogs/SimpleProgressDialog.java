package com.despectra.android.journal.Dialogs;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

/**
 * Created by Dmitry on 27.03.14.
 */
public class SimpleProgressDialog extends DialogFragment {
    public static final String KEY_MESSAGE = "message";

    private String mMessage;

    public static SimpleProgressDialog newInstance(String message) {
        SimpleProgressDialog dialog = new SimpleProgressDialog();
        Bundle args = new Bundle();
        args.putString(KEY_MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMessage = getArguments().getString(KEY_MESSAGE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
        dialog.setMessage(mMessage);
        return dialog;
    }

    public void setMessage(String message) {
        mMessage = message;
        if (getDialog() != null) {
            ((ProgressDialog)getDialog()).setMessage(mMessage);
        }
    }
}
