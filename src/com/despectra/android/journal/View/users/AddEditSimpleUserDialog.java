package com.despectra.android.journal.view.users;

import android.app.AlertDialog;
import android.os.Parcel;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import com.despectra.android.journal.R;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.view.AddEditDialog;

/**
 * Created by Dmitry on 16.04.2014.
 */

public class AddEditSimpleUserDialog extends AddEditDialog {
    public static final String TAG = "AddEditSimpleUserDialog";
    public static final String FRAGMENT_TAG = TAG;

    private EditText mNameEdit;
    private EditText mLoginEdit;
    private EditText mSurnameEdit;
    private EditText mMiddlenameEdit;
    private DialogListener mListener;

    public static AddEditSimpleUserDialog newInstance(JoinedEntityIds userIds, String addTitle, String editTitle,
                                                      String firstName, String middleName, String secondName, String login) {
        AddEditSimpleUserDialog dialog = new AddEditSimpleUserDialog();
        dialog.prepareAllArguments(R.layout.dialog_add_student,
                addTitle,
                editTitle,
                "Отмена",
                "Добавить и закрыть",
                "Сохранить",
                "Добавить и продолжить",
                new SimpleUserDialogData(userIds, firstName, middleName, secondName, login));
        return dialog;
    }

    @Override
    protected void completeDialogCreation(AlertDialog.Builder builder) {
        mNameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_name);
        mSurnameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_surname);
        mMiddlenameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_middlename);
        mLoginEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_login);

        SimpleUserDialogData data = (SimpleUserDialogData)mDialogData;
        mNameEdit.setText(data.firstName);
        mMiddlenameEdit.setText(data.middleName);
        mSurnameEdit.setText(data.secondName);
        mLoginEdit.setText(data.login);

        mLoginEdit.setEnabled(inAddMode());
    }

    @Override
    protected void clearDialogView() {
        mNameEdit.setText("");
        mSurnameEdit.setText("");
        mMiddlenameEdit.setText("");
        mLoginEdit.setText("");
    }

    @Override
    protected void respondNotValidated() {
        Toast.makeText(getActivity(), "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected boolean validateInputData() {
        return !(TextUtils.isEmpty(mNameEdit.getText()) || TextUtils.isEmpty(mSurnameEdit.getText())
                || TextUtils.isEmpty(mMiddlenameEdit.getText()) || TextUtils.isEmpty(mLoginEdit.getText()));
    }

    @Override
    protected void onNeutralClicked(int mode) {
        if (mListener != null) {
            if (inAddMode()) {
                mListener.onAddUser(mNameEdit.getText().toString(), mMiddlenameEdit.getText().toString(),
                        mSurnameEdit.getText().toString(), mLoginEdit.getText().toString());
            } else {
                SimpleUserDialogData data = (SimpleUserDialogData) mDialogData;
                mListener.onEditUser(data.userIds,
                        data.firstName, mNameEdit.getText().toString(),
                        data.middleName, mMiddlenameEdit.getText().toString(),
                        data.secondName, mSurnameEdit.getText().toString());
            }
        }
    }

    @Override
    protected void onPositiveClicked(int mode) {
        onNeutralClicked(mode);
    }

    public void setDialogListener(DialogListener listener) {
        mListener = listener;
    }

    public static final class SimpleUserDialogData extends DialogData {

        public JoinedEntityIds userIds;
        public String firstName;
        public String middleName;
        public String secondName;
        public String login;

        public SimpleUserDialogData(JoinedEntityIds userIds, String firstName, String middleName, String secondName, String login) {
            this.userIds = userIds;
            this.firstName = firstName;
            this.middleName = middleName;
            this.secondName = secondName;
            this.login = login;
        }

        public SimpleUserDialogData(Parcel parcel) {
            userIds = JoinedEntityIds.fromBundle(parcel.readBundle());
            firstName = parcel.readString();
            middleName = parcel.readString();
            secondName = parcel.readString();
            login = parcel.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeBundle(userIds.toBundle());
            parcel.writeString(firstName);
            parcel.writeString(middleName);
            parcel.writeString(secondName);
            parcel.writeString(login);
        }

        public final Creator<SimpleUserDialogData> CREATOR
                = new Creator<SimpleUserDialogData>() {
            @Override
            public SimpleUserDialogData createFromParcel(Parcel parcel) {
                return new SimpleUserDialogData(parcel);
            }

            @Override
            public SimpleUserDialogData[] newArray(int size) {
                return new SimpleUserDialogData[size];
            }
        };
    }

    public interface DialogListener {
        public void onAddUser(String firstName, String middleName, String secondName, String login);
        public void onEditUser(JoinedEntityIds userIds,
                                  String oldFirstName, String newFirstName,
                                  String oldMiddleName, String newMiddleName,
                                  String oldSecondName, String newSecondName);
    }
}