package com.despectra.android.journal.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.despectra.android.journal.R;

/**
 * Created by Dmitry on 08.04.14.
 */
public class AddEditGroupDialog extends AddEditDialog {

    public static final String FRAGMENT_TAG = "addGroupDialog";
    public static final String KEY_LOCAL_GROUP_ID = "locgroupId";
    public static final String KEY_REMOTE_GROUP_ID = "remgroupId";
    public static final String KEY_GROUP_TEXT = "group";

    private EditText mGroupNameEdit;
    private long mLocalGroupId;
    private long mRemoteGroupId;
    private String mGroupText;

    public AddEditGroupDialog() {
        super();
    }

    public static AddEditGroupDialog newInstance(String additionTitle, String editionTitle, String groupText, long localGroupId, long remoteGroupId) {
        AddEditGroupDialog dialog = new AddEditGroupDialog();
        dialog.init(R.layout.dialog_add_group, additionTitle, editionTitle, groupText, localGroupId, remoteGroupId);
        return dialog;
    }

    public void setGroupIds(long localId, long remoteId) {
        mLocalGroupId = localId;
        mRemoteGroupId = remoteId;
    }

    public void setGroupText(String text) {
        mGroupText = text;
        if (mGroupNameEdit != null) {
            mGroupNameEdit.setText(text);
        }
    }


    @Override
    protected AddEditDialog init(int mainViewId, String additionTitle, String editionTitle, Object... parameters) {
        Bundle args = super.getBaseArgs(this, mainViewId, additionTitle, editionTitle);
        args.putString(KEY_GROUP_TEXT, (String) parameters[0]);
        args.putLong(KEY_LOCAL_GROUP_ID, (Long) parameters[1]);
        args.putLong(KEY_REMOTE_GROUP_ID, (Long) parameters[2]);
        setArguments(args);
        return this;
    }

    @Override
    protected void setCustomArgs(Bundle arguments) {
        mLocalGroupId = arguments.getLong(KEY_LOCAL_GROUP_ID);
        mRemoteGroupId = arguments.getLong(KEY_REMOTE_GROUP_ID);
        mGroupText = arguments.getString(KEY_GROUP_TEXT);
    }

    @Override
    protected Dialog completeDialogCreation(AlertDialog.Builder builder) {
        mGroupNameEdit = (EditText) mMainView.findViewById(R.id.group_dialog_field_name);
        if (mMode == MODE_EDIT) {
            mGroupNameEdit.setText(mGroupText);
        }
        String positiveBtnText = (mMode == MODE_ADD) ? "Добавить" : "Обновить";
        builder.setNegativeButton("Отмена", null)
                .setPositiveButton(positiveBtnText, null);
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
                    String groupName = mGroupNameEdit.getText().toString();
                    if (groupName.isEmpty()) {
                        Toast.makeText(getActivity(), "Имя класса не может быть пустым", Toast.LENGTH_SHORT).show();
                    } else {
                        if (mListener != null) {
                            mListener.onPositiveClicked(mMode, groupName, mLocalGroupId, mRemoteGroupId);
                        }
                        dismiss();
                    }
                }
            });
        }
    }
}
