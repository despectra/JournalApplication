package com.despectra.android.journal.view.journal;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.R;
import com.despectra.android.journal.model.Mark;
import com.despectra.android.journal.view.PseudoGridAdapter;

/**
 * Created by Dmitry on 18.03.14.
 */
public class MarksAdapter extends PseudoGridAdapter<Mark> {

    private int mMarkLocalIdColIndex;
    private int mMarkRemIdColIndex;
    private int mLessonDateColIndex;
    private int mLessonIdColIndex;
    private int mStudentIdColIndex;
    private int mMarkColIndex;
    private int mMarkStatusColIndex;

    public MarksAdapter(Context context, Cursor cursor) {
        super(context, cursor, 6, 6, new int[]{0, 0, 0, 0, 0, 0});
    }

    //@Override
    //public void bindSingleViewToData(int row, int column, View view, Object dataObject) {
        /*TextView markView = (TextView) view;
        MarkHolder mark = (MarkHolder) dataObject;
        String m = mark.mark;
        int s = mark.status;
        int color;
        if (m.equals("-1")) {
            markView.setText("");
            markView.setBackgroundResource(R.drawable.mark_item_background_selector);
            return;
        }
        if (s != Contract.STATUS_IDLE) {
            color = getContext().getResources().getColor(R.color.lesson_future_stroke);
        } else if (m.equals("1")) {
            color = getContext().getResources().getColor(R.color.mark_1);
        } else if (m.equals("2")) {
            color = getContext().getResources().getColor(R.color.mark_2);
        } else if (m.equals("3")) {
            color = getContext().getResources().getColor(R.color.mark_3);
        } else if (m.equals("4")) {
            color = getContext().getResources().getColor(R.color.mark_4);
        } else if (m.equals("5")) {
            color = getContext().getResources().getColor(R.color.mark_5);
        } else if (m.equals("0")) {
            color = getContext().getResources().getColor(R.color.gray_a0);
            m = "н";
        } else {
            color = Color.WHITE;
        }
        LayerDrawable layer = new LayerDrawable(new Drawable[]{getContext().getResources().getDrawable(R.drawable.mark_item_background_selector), new ColorDrawable(color)});
        markView.setBackground(layer);
        markView.setText(m);
        markView.setEnabled(s != Contract.STATUS_IDLE);*/
   // }


    @Override
    public Cursor swapCursor(Cursor c) {
        mMarkLocalIdColIndex = c.getColumnIndexOrThrow(Marks._ID);
        mMarkRemIdColIndex = c.getColumnIndexOrThrow(Marks.REMOTE_ID);
        mLessonIdColIndex = c.getColumnIndexOrThrow(Marks.FIELD_LESSON_ID);
        mLessonDateColIndex = c.getColumnIndexOrThrow(Lessons.FIELD_DATE);
        mStudentIdColIndex = c.getColumnIndexOrThrow(Marks.FIELD_STUDENT_ID);
        mMarkColIndex = c.getColumnIndexOrThrow(Marks.FIELD_MARK);
        mMarkStatusColIndex = c.getColumnIndexOrThrow(Marks.ENTITY_STATUS);
        return super.swapCursor(c);
    }

    @Override
    protected void convertCursorItemToMatrixRow(Cursor cursor) {

    }

    @Override
    protected boolean isDataColumn(int column) {
        return true;
    }

    @Override
    protected View newNonDataColumn(int column) {
        return null;
    }

    @Override
    protected View newDataColumn(ViewGroup parent) {
        return View.inflate(getContext(), R.layout.journal_mark_item, parent);
    }

    @Override
    protected void bindNonDataColumn(int row, int column, View cell) {
    }

    @Override
    protected void bindDataColumn(int row, int column, View cell) {
        Cursor cursor = getCursor();
        if (cursor != null && cursor.getCount() > 0) {

        }
    }
}
