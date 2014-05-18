package com.despectra.android.journal.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by Dmitry on 11.04.14.
 */
public class SimpleConfirmationDialog extends DialogFragment {

    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TITLE = "title";

    private String mTitle;
    private String mMessage;
    private OnConfirmListener mListener;

    public static SimpleConfirmationDialog newInstance(String title, String message) {
        SimpleConfirmationDialog dialog = new SimpleConfirmationDialog();
        Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        args.putString(KEY_MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    public SimpleConfirmationDialog() {
        super();
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString(KEY_TITLE);
            mMessage = savedInstanceState.getString(KEY_MESSAGE);
        } else {
            mTitle = getArguments().getString(KEY_TITLE);
            mMessage = getArguments().getString(KEY_MESSAGE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(mTitle)
                .setMessage(mMessage)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mListener != null) {
                            mListener.onConfirm();
                        }
                        dialogInterface.dismiss();
                    }
                })
                .setNeutralButton("Отмена", null)
                .create();
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_TITLE, mTitle);
        outState.putString(KEY_MESSAGE, mMessage);
    }

    public interface OnConfirmListener {
        public void onConfirm();
    }
}
