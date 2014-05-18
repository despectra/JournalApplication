package com.despectra.android.journal.view.journal;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.AddEditDialog;

/**
 * Created by Dmitry on 24.04.2014.
 */
public class AddEditMark extends AddEditDialog {

    public static final String FRAGMENT_TAG = "AddEditMark";

    private long mMarkId;
    private int mMark;

    private Button[] mMarksBtns;

    public static AddEditMark newInstance(long markId, int mark) {
        AddEditMark dialog = new AddEditMark();
        dialog.init(R.layout.dialog_add_mark, "Оценка", "Редактирование оценки", markId, mark);
        return dialog;
    }

    public void setData(long markId, int mark) {
        mMarkId = markId;
        mMark = mark;
    }

    @Override
    protected AddEditDialog init(int mainViewId, String additionTitle, String editionTitle, Object... parameters) {
        Bundle args = getBaseArgs(this, mainViewId, additionTitle, editionTitle);
        args.putLong("markId", (Long)parameters[0]);
        args.putInt("mark", (Integer) parameters[1]);
        setArguments(args);
        return this;
    }

    @Override
    protected void setCustomArgs(Bundle arguments) {
        mMarkId = arguments.getLong("markId");
        mMark = arguments.getInt("mark");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mMarkId = savedInstanceState.getLong("markId");
            mMark = savedInstanceState.getInt("mark");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("markId", mMarkId);
        outState.putInt("mark", mMark);
    }

    @Override
    protected Dialog completeDialogCreation(AlertDialog.Builder builder) {
        mMarksBtns = new Button[6];
        mMarksBtns[0] = (Button) mMainView.findViewById(R.id.mark_epson);
        mMarksBtns[1] = (Button) mMainView.findViewById(R.id.mark_1);
        mMarksBtns[2] = (Button) mMainView.findViewById(R.id.mark_2);
        mMarksBtns[3] = (Button) mMainView.findViewById(R.id.mark_3);
        mMarksBtns[4] = (Button) mMainView.findViewById(R.id.mark_4);
        mMarksBtns[5] = (Button) mMainView.findViewById(R.id.mark_5);

        for (int i = 0; i < 6; i++) {
            final int ii = i;
            mMarksBtns[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mMark >= 0) {
                        mMarksBtns[mMark].setActivated(false);
                    }
                    mMark = ii;
                    mMarksBtns[mMark].setActivated(true);
                }
            });
        }

        if (mMark >= 0) {
            mMarksBtns[mMark].setActivated(true);
        }

        builder.setNegativeButton("Отмена", null)
                .setPositiveButton("OK", null);

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
                    if (mMark < 0) {
                        Toast.makeText(getActivity(), "Выберите оценку", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int prevMark = mMark;
                    if (mListener != null) {
                        mListener.onPositiveClicked(mMode, mMarkId, mMark, mDontClose);
                    }
                    if (mDontClose) {
                        mMarksBtns[prevMark].setActivated(false);
                        mMark = -1;
                    } else {
                        dismiss();
                    }
                }
            });
        }
    }
}
