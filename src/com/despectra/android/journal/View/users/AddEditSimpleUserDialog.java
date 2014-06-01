package com.despectra.android.journal.view.users;

import android.app.AlertDialog;
import android.os.Parcel;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import com.despectra.android.journal.R;
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
    private StudentDialogListener mListener;

    public static AddEditSimpleUserDialog newInstance(long localId, long remoteId, String firstName,
                                                   String middleName, String secondName, String login) {
        AddEditSimpleUserDialog dialog = new AddEditSimpleUserDialog();
        dialog.prepareAllArguments(R.layout.dialog_add_student,
                "Добавление ученика",
                "Редактирование ученика",
                "Отмена",
                "Добавить и закрыть",
                "Сохранить",
                "Добавить и продолжить",
                new StudentDialogData(localId, remoteId, firstName, middleName, secondName, login));
        return dialog;
    }

    @Override
    protected void completeDialogCreation(AlertDialog.Builder builder) {
        mNameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_name);
        mSurnameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_surname);
        mMiddlenameEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_middlename);
        mLoginEdit = (EditText) mMainView.findViewById(R.id.student_dialog_field_login);

        StudentDialogData data = (StudentDialogData)mDialogData;
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
                mListener.onAddStudent(mNameEdit.getText().toString(), mMiddlenameEdit.getText().toString(),
                        mSurnameEdit.getText().toString(), mLoginEdit.getText().toString());
            } else {
                StudentDialogData data = (StudentDialogData) mDialogData;
                mListener.onEditStudent(data.localId, data.remoteId,
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

    public void setStudentDialogListener(StudentDialogListener listener) {
        mListener = listener;
    }

    public static final class StudentDialogData extends DialogData {

        public long localId;
        public long remoteId;
        public String firstName;
        public String middleName;
        public String secondName;
        public String login;

        public StudentDialogData(long localId, long remoteId, String firstName, String middleName, String secondName, String login) {
            this.localId = localId;
            this.remoteId = remoteId;
            this.firstName = firstName;
            this.middleName = middleName;
            this.secondName = secondName;
            this.login = login;
        }

        public StudentDialogData(Parcel parcel) {
            localId = parcel.readLong();
            remoteId = parcel.readLong();
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
            parcel.writeLong(localId);
            parcel.writeLong(remoteId);
            parcel.writeString(firstName);
            parcel.writeString(middleName);
            parcel.writeString(secondName);
            parcel.writeString(login);
        }

        public final Creator<StudentDialogData> CREATOR
                = new Creator<StudentDialogData>() {
            @Override
            public StudentDialogData createFromParcel(Parcel parcel) {
                return new StudentDialogData(parcel);
            }

            @Override
            public StudentDialogData[] newArray(int size) {
                return new StudentDialogData[size];
            }
        };
    }

    public interface StudentDialogListener {
        public void onAddStudent(String firstName, String middleName, String secondName, String login);
        public void onEditStudent(long localId, long remoteId,
                                  String oldFirstName, String newFirstName,
                                  String oldMiddleName, String newMiddleName,
                                  String oldSecondName, String newSecondName);
    }
}