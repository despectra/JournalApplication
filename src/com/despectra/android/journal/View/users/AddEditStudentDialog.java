package com.despectra.android.journal.view.users;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.AddEditDialog;

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
    private static final String KEY_REMOTE_ID = "remoteId";

    private EditText mNameEdit;
    private EditText mLoginEdit;
    private EditText mSurnameEdit;
    private EditText mMiddlenameEdit;
    private String mName;
    private String mSurname;
    private String mMiddlename;
    private String mLogin;

    private long mLocalId;
    private long mRemoteId;

    public AddEditStudentDialog() {
        super();
    }

    public static AddEditStudentDialog newInstance(String additionTitle, String editionTitle,
                                                   long studentLocalId, long studentRemoteId,
                                                   String name, String middlename, String surname, String login) {
        AddEditStudentDialog dialog = new AddEditStudentDialog();
        dialog.init(R.layout.dialog_add_student, additionTitle, editionTitle, studentLocalId, studentRemoteId, name, middlename, surname, login);
        return dialog;
    }

    public void setStudentIds(long localId, long remoteId) {
        mLocalId = localId;
        mRemoteId = remoteId;
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
        args.putLong(KEY_REMOTE_ID, (Long)parameters[1]);
        args.putString(KEY_NAME, (String) parameters[2]);
        args.putString(KEY_MIDDLENAME, (String) parameters[3]);
        args.putString(KEY_SURNAME, (String) parameters[4]);
        args.putString(KEY_LOGIN, (String) parameters[5]);
        setArguments(args);
        return this;
    }

    @Override
    protected void setCustomArgs(Bundle arguments) {
        mName = arguments.getString(KEY_NAME);
        mSurname = arguments.getString(KEY_SURNAME);
        mMiddlename = arguments.getString(KEY_MIDDLENAME);
        mLocalId = arguments.getLong(KEY_LOCAL_ID);
        mRemoteId = arguments.getLong(KEY_REMOTE_ID);
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

                        dialogInterface.dismiss();
                    }
                });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button button = dialog.getButton(Dialog.BUTTON_POSITIVE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String name = mNameEdit.getText().toString();
                    String surname = mSurnameEdit.getText().toString();
                    String middlename = mMiddlenameEdit.getText().toString();
                    String login = mLoginEdit.getText().toString();
                    if (name.isEmpty() || surname.isEmpty() || login.isEmpty() || middlename.isEmpty()) {
                        Toast.makeText(getActivity(), "Все поля длжны быть заполнены", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mListener != null) {
                        mListener.onPositiveClicked(mMode, mLocalId, name, middlename, surname, login);
                    }
                    if (mDontClose) {
                        mNameEdit.setText("");
                        mSurnameEdit.setText("");
                        mMiddlenameEdit.setText("");
                        mLoginEdit.setText("");
                        return;
                    }
                    dismiss();
                }
            });
        }
    }
}
