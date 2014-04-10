package com.despectra.android.journal.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import com.despectra.android.journal.R;

import java.util.zip.Inflater;

/**
 * Created by Dmitry on 08.04.14.
 */
public class AddGroupDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "addGroupDialog";

    private EditText mGroupNameEdit;
    private PositiveClickListener mListener;

    public AddGroupDialog() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_add_group, null);
        mGroupNameEdit = (EditText) dialogView.findViewById(R.id.group_dialog_field_name);
        builder.setTitle("Добавить новый класс")
                .setView(dialogView)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String groupName = mGroupNameEdit.getText().toString();
                        if (mListener != null) {
                            mListener.onAddGroup(groupName);
                        }
                        dialogInterface.dismiss();
                    }
                });
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void setPositiveCLickListener(PositiveClickListener listener) {
        mListener = listener;
    }

    public interface PositiveClickListener {
        public void onAddGroup(String name);
    }
}
