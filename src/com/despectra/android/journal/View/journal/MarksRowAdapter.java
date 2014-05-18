package com.despectra.android.journal.view.journal;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.TextView;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.HorizontalViewsRowAdapter;

/**
 * Created by Dmitry on 18.03.14.
 */
public class MarksRowAdapter extends HorizontalViewsRowAdapter {

    public MarksRowAdapter(Context c, int columnsCount, MarkHolder[] data) {
        super(c, R.layout.journal_mark_item, columnsCount, data);
    }

    @Override
    public void defineNewViewHolder(int row, int column, View view) {
    }

    @Override
    public void bindSingleViewToData(int row, int column, View view, Object dataObject) {
        TextView markView = (TextView) view;
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
        markView.setEnabled(s != Contract.STATUS_IDLE);
    }

    public void swapMarks(MarkHolder[] marks) {
        setData(marks, true);
    }

    public static class MarkHolder {
        public long localId;
        public long remoteId;
        public int status;
        public String mark;
    }
}
