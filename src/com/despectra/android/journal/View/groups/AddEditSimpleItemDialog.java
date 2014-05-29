package com.despectra.android.journal.view.groups;

import android.app.AlertDialog;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.EditText;
import android.widget.Toast;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.AddEditDialog;

/**
 * Created by Dmitry on 08.04.14.
 */

public class AddEditSimpleItemDialog extends AddEditDialog {

    public static final String FRAGMENT_TAG = "addGroupDialog";

    private DialogListener mListener;
    private EditText mItemNameEdit;

    public static AddEditSimpleItemDialog newInstance(String dialogAddTitle, String dialogEditTitle, String itemText, long localGroupId, long remoteGroupId) {
        AddEditSimpleItemDialog dialog = new AddEditSimpleItemDialog();
        dialog.prepareAllArguments(R.layout.dialog_add_group,
                dialogAddTitle,
                dialogEditTitle,
                "Отмена",
                "Добавить и закрыть",
                "Сохранить",
                "Добавить и продолжить",
                new SimpleItemDialogData(localGroupId, remoteGroupId, itemText));
        return dialog;
    }

    @Override
    protected void completeDialogCreation(AlertDialog.Builder builder) {
        mItemNameEdit = (EditText) mMainView.findViewById(R.id.group_dialog_field_name);
        mItemNameEdit.setText(((SimpleItemDialogData)mDialogData).itemName);
    }

    @Override
    protected void clearDialogView() {
        mItemNameEdit.setText("");
    }

    @Override
    protected void respondNotValidated() {
        Toast.makeText(getActivity(), "Имя класса не может быть пустым", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected boolean validateInputData() {
        return !mItemNameEdit.getText().toString().isEmpty();
    }

    public void setDialogListener(DialogListener listener) {
        mListener = listener;
    }

    @Override
    protected void onNeutralClicked(int mode) {
        if (mListener != null) {
            SimpleItemDialogData data = (SimpleItemDialogData)mDialogData;
            if (inAddMode()) {
                mListener.onAddItem(mItemNameEdit.getText().toString());
            } else {
                mListener.onEditItem(mItemNameEdit.getText().toString(), data.localId, data.remoteId);
            }
        }
    }

    @Override
    protected void onPositiveClicked(int mode) {
        onNeutralClicked(mMode);
    }

    public static class SimpleItemDialogData extends DialogData {

        public long localId;
        public long remoteId;
        public String itemName;

        public SimpleItemDialogData(long localId, long remoteId, String itemName) {
            this.localId = localId;
            this.remoteId = remoteId;
            this.itemName = itemName;
        }

        public SimpleItemDialogData(Parcel in) {
            this.localId = in.readLong();
            this.remoteId = in.readLong();
            this.itemName = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(localId);
            parcel.writeLong(remoteId);
            parcel.writeString(itemName);
        }

        public static final Parcelable.Creator<SimpleItemDialogData> CREATOR
                = new Parcelable.Creator<SimpleItemDialogData>() {
            public SimpleItemDialogData createFromParcel(Parcel in) {
                return new SimpleItemDialogData(in);
            }

            public SimpleItemDialogData[] newArray(int size) {
                return new SimpleItemDialogData[size];
            }
        };
    }

    public interface DialogListener {
        public void onAddItem(String name);
        public void onEditItem(String name, long localId, long remoteId);
    }
}