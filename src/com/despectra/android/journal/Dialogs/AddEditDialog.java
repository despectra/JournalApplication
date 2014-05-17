package com.despectra.android.journal.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by Dmitry on 10.04.14.
 */
public abstract class AddEditDialog extends DialogFragment {

    public static final int MODE_ADD = 0;
    public static final int MODE_EDIT = 1;
    public static final String KEY_MODE = "mode";
    public static final String KEY_MAIN_VIEW_ID = "mainView";
    public static final String KEY_ADDITION_TITLE = "addtitle";
    public static final String KEY_EDITION_TITLE = "edittitle";
    public static final String KEY_POS_BTN_TEXT = "posBtnText";
    public static final String KEY_NEUTRAL_BTN_TEXT = "neutralBtnText";
    private static final String KEY_DONT_CLOSE = "dontclose";

    protected DialogButtonsListener mListener;
    protected View mMainView;
    protected int mMode;
    protected int mMainViewId;
    protected boolean mDontClose;
    protected String mAdditionTitle;
    protected String mEditionTitle;
    protected String mPositiveBtnText;
    protected String mNeutralBtnText;

    protected abstract AddEditDialog init(int mainViewId, String additionTitle, String editionTitle, Object... parameters);

    protected Bundle getBaseArgs(AddEditDialog dialog, int mainViewId, String additionTitle, String editionTitle) {
        Bundle args = dialog.getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(KEY_MAIN_VIEW_ID, mainViewId);
        args.putString(KEY_ADDITION_TITLE, additionTitle);
        args.putString(KEY_EDITION_TITLE, editionTitle);
        return args;
    }

    protected Bundle setBaseArgs() {
        Bundle args = getArguments();
        mMainViewId = args.getInt(KEY_MAIN_VIEW_ID);
        mAdditionTitle = args.getString(KEY_ADDITION_TITLE);
        mEditionTitle = args.getString(KEY_EDITION_TITLE);
        return args;
    }

    public AddEditDialog() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mMode = savedInstanceState.getInt(KEY_MODE);
            mDontClose = savedInstanceState.getBoolean(KEY_DONT_CLOSE);
        }
    }

    protected abstract void setCustomArgs(Bundle arguments);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCustomArgs(setBaseArgs());
        mMainView = LayoutInflater.from(getActivity()).inflate(mMainViewId, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(mMode == MODE_ADD ? mAdditionTitle : mEditionTitle)
                .setView(mMainView);
        if (mMode == MODE_ADD) {
            builder.setMultiChoiceItems(new String[]{"Не закрывать диалог при добавлении"}, new boolean[]{false}, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                    mDontClose = b;
                }
            });
        }
        return completeDialogCreation(builder);
    }

    protected abstract Dialog completeDialogCreation(AlertDialog.Builder builder);

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_MODE, mMode);
        outState.putBoolean(KEY_DONT_CLOSE, mDontClose);
    }

    public void showInMode(int mode, FragmentManager fm, String tag) {
        mMode = mode;
        super.show(fm, tag);
    }

    public void setDialogListener(DialogButtonsListener listener) {
        mListener = listener;
    }

    public interface DialogButtonsListener {
        public void onPositiveClicked(int mode, Object... args);
        public void onNegativeClicked(int mode, Object... args);
        public void onNeutralClicked(int mode, Object... args);
    }

    public static abstract class DialogButtonsAdapter implements DialogButtonsListener {
        @Override
        public void onPositiveClicked(int mode, Object... args) {
        }

        @Override
        public void onNegativeClicked(int mode, Object... args) {
        }

        @Override
        public void onNeutralClicked(int mode, Object... args) {
        }
    }

}
