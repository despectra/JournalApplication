package com.despectra.android.journal.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.opengl.ETC1;
import android.os.Bundle;
import android.widget.EditText;
import com.despectra.android.journal.R;

/**
 * Created by Dmitry on 16.04.2014.
 */
public class AddEditStudentDialog extends AddEditDialog {
    public static final String TAG = "AddEditStudentDialog";
    public static final String KEY_NAME = "name";
    public static final String KEY_MIDDLENAME = "middlename";
    public static final String KEY_SURNAME = "surname";
    public static final String KEY_LOGIN = "login";
    public static final String KEY_LOCAL_ID = "localId";

    private EditText mNameEdit;
    private EditText mLoginEdit;
    private EditText mSurnameEdit;
    private EditText mMiddlenameEdit;
    private String mName;
    private String mSurname;
    private String mMiddlename;
    private String mLogin;
    private long mLocalId;

    public AddEditStudentDialog() {
        super();
    }

    public static AddEditStudentDialog newInstance(String additionTitle, String editionTitle,
                                                   long studentLocalId, String name, String middlename, String surname, String login) {
        AddEditStudentDialog dialog = new AddEditStudentDialog();
        dialog.init(R.layout.dialog_add_student, additionTitle, editionTitle, studentLocalId, name, middlename, surname, login);
        return dialog;
    }

    public void setStudentLocalId(long localId) {
        mLocalId = localId;
    }

    public void setData(String name, String middlename, String surname, String login) {
        mName = name;
        mMiddlename = middlename;
        mSurname = surname;
        mLogin = login;
        if (mNameEdit != null) {
            mNameEdit.setText(mName);
        }

        if (mSurnameEdit != null) {
            mSurnameEdit.setText(mSurname);
        }

        if (mMiddlenameEdit != null) {
            mMiddlenameEdit.setText(mMiddlename);
        }
        
        if (mLoginEdit != null) {
            mLoginEdit.setText(mLogin);
        }
    }

    @Override
    protected AddEditDialog init(int mainViewId, String additionTitle, String editionTitle, Object... parameters) {
        Bundle args = super.getBaseArgs(this, mainViewId, additionTitle, editionTitle);
        args.putLong(KEY_LOCAL_ID, (Long)parameters[0]);
        args.putString(KEY_NAME, (String) parameters[1]);
        args.putString(KEY_MIDDLENAME, (String) parameters[2]);
        args.putString(KEY_SURNAME, (String) parameters[3]);
        args.putString(KEY_LOGIN, (String) parameters[4]);
        setArguments(args);
        return this;
    }

    @Override
    protected void setCustomArgs(Bundle arguments) {
        mName = arguments.getString(KEY_NAME);
        mSurname = arguments.getString(KEY_SURNAME);
        mMiddlename = arguments.getString(KEY_MIDDLENAME);
        mLocalId = arguments.getLong(KEY_LOCAL_ID);
        mLogin = arguments.getString(KEY_LOGIN);
    }

    @Override
    protected Dialog completeDialogCreation(AlertDialog.Builder builder) {
        mNameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_name);
        mSurnameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_surname);
        mMiddlenameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_middlename);
        mLoginEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_login);

        mLoginEdit.setEnabled(mMode == MODE_ADD);
        if (mMode == MODE_EDIT) {
            mNameEdit.setText(mName);
            mSurnameEdit.setText(mSurname);
            mMiddlenameEdit.setText(mMiddlename);
            mLoginEdit.setText(mLogin);
        }
        String positiveBtnText = (mMode == MODE_ADD) ? "Добавить" : "Обновить";
        builder.setNegativeButton("Отмена", null)
                .setPositiveButton(positiveBtnText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = mNameEdit.getText().toString();
                        String surname = mSurnameEdit.getText().toString();
                        String middlename = mMiddlenameEdit.getText().toString();
                        String login = mLoginEdit.getText().toString();
                        if (mListener != null) {
                            mListener.onPositiveClicked(mMode, mLocalId, name, middlename, surname, login);
                        }
                        dialogInterface.dismiss();
                    }
                });
        return builder.create();
    }
}
