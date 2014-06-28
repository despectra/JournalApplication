package com.despectra.android.journal.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

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
    public static final String KEY_NEG_BTN_TEXT = "negBtnText";
    public static final String KEY_POS_BTN_TEXT = "posBtnText";
    public static final String KEY_NEUTRAL_ADD_BTN_TEXT = "neutralAddBtnText";
    public static final String KEY_NEUTRAL_EDIT_BTN_TEXT = "neutralEditBtnText";
    public static final String KEY_DIALOG_DATA = "dialogData";
    private static final String KEY_USE_CONTINUE_BUTTON = "useContBtn";

    protected View mMainView;
    protected int mMode;
    protected int mMainViewId;
    protected boolean mUseContinueBtn;
    protected String mAdditionTitle;
    protected String mEditionTitle;
    protected String mNegativeBtnText;
    protected String mNeutralAddBtnText;
    protected String mNeutralEditBtnText;
    protected String mPositiveBtnText;
    protected DialogData mDialogData;

    protected void prepareAllArguments(int mainViewId,
                                     String additionTitle,
                                     String editionTitle,
                                     String negativeBtnText,
                                     String neutralAddBtnText,
                                     String neutralEditBtnText,
                                     String positiveBtnText,
                                     DialogData dialogData) {
        Bundle args = prepareBaseArguments(mainViewId,
                additionTitle,
                editionTitle,
                negativeBtnText,
                neutralAddBtnText,
                neutralEditBtnText,
                positiveBtnText,
                dialogData);
        setArguments(args);
    }

    private Bundle prepareBaseArguments(int mainViewId,
                                          String additionTitle,
                                          String editionTitle,
                                          String negativeBtnText,
                                          String neutralAddBtnText,
                                          String neutralEditBtnText,
                                          String positiveBtnText,
                                          DialogData dialogData) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(KEY_MAIN_VIEW_ID, mainViewId);
        args.putString(KEY_ADDITION_TITLE, additionTitle);
        args.putString(KEY_EDITION_TITLE, editionTitle);
        args.putString(KEY_NEG_BTN_TEXT, negativeBtnText);
        args.putString(KEY_NEUTRAL_ADD_BTN_TEXT, neutralAddBtnText);
        args.putString(KEY_NEUTRAL_EDIT_BTN_TEXT, neutralEditBtnText);
        args.putString(KEY_POS_BTN_TEXT, positiveBtnText);
        args.putParcelable(KEY_DIALOG_DATA, dialogData);
        return args;
    }

    protected Bundle applyBaseArguments() {
        Bundle args = getArguments();
        mMainViewId = args.getInt(KEY_MAIN_VIEW_ID);
        mAdditionTitle = args.getString(KEY_ADDITION_TITLE);
        mEditionTitle = args.getString(KEY_EDITION_TITLE);
        mNegativeBtnText = args.getString(KEY_NEG_BTN_TEXT);
        mNeutralAddBtnText = args.getString(KEY_NEUTRAL_ADD_BTN_TEXT);
        mNeutralEditBtnText = args.getString(KEY_NEUTRAL_EDIT_BTN_TEXT);
        mPositiveBtnText = args.getString(KEY_POS_BTN_TEXT);
        mDialogData = args.getParcelable(KEY_DIALOG_DATA);
        mUseContinueBtn = args.getBoolean(KEY_USE_CONTINUE_BUTTON, false);
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
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        applyBaseArguments();
        mMainView = LayoutInflater.from(getActivity()).inflate(mMainViewId, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(inAddMode() ? mAdditionTitle : mEditionTitle)
                .setView(mMainView)
                .setNegativeButton(mNegativeBtnText, null)
                .setNeutralButton(inAddMode() ? mNeutralAddBtnText : mNeutralEditBtnText, null);
        if (inAddMode()) {
            if (mUseContinueBtn) {
                builder.setPositiveButton(mPositiveBtnText, null);
            }
        }
        completeDialogCreation(builder);
        Dialog dialog = builder.create();
        if (inAddMode()) {
            clearDialogView();
        }
        return dialog;
    }

    protected abstract void completeDialogCreation(AlertDialog.Builder builder);

    public boolean inAddMode() {
        return mMode == MODE_ADD;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_MODE, mMode);
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE);
            Button neutralButton = dialog.getButton(Dialog.BUTTON_NEUTRAL);
            Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);

            if (negativeButton != null) {
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onNegativeClicked(mMode);
                        dismiss();
                    }
                });
            }
            if (neutralButton != null) {
                neutralButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (validateInputData()) {
                            onNeutralClicked(mMode);
                            dismiss();
                        } else {
                            respondNotValidated();
                        }
                    }
                });
            }
            if (positiveButton != null) {
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (validateInputData()) {
                            onPositiveClicked(mMode);
                            clearDialogView();
                        } else {
                            respondNotValidated();
                        }
                    }
                });
            }
        }
    }

    protected void onNegativeClicked(int mode) {
    }

    protected void onNeutralClicked(int mode) {
    }

    protected void onPositiveClicked(int mode) {
    }

    protected abstract void clearDialogView();

    protected abstract void respondNotValidated();

    protected abstract boolean validateInputData();

    public void showInMode(int mode, FragmentManager fm, String tag) {
        mMode = mode;
        super.show(fm, tag);
    }

    public static abstract class DialogData implements Parcelable {}
}
