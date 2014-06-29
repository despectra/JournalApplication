package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Matrix;
import android.support.v4.widget.CursorAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.FixedColumnsMatrix;

/**
 * Created by Dmitry on 18.06.14.
 */
public abstract class PseudoGridAdapter<T> extends CursorAdapter {

    private Context mContext;
    private int mColumnsCount;
    private int mDataColumnsCount;
    private int[] mColumnsWidths;
    protected FixedColumnsMatrix<T> mData;
    protected OnCellClickedListener mCellClickedListener;

    public PseudoGridAdapter(Context context, Cursor cursor, int columnsCount, int dataColsCount, int[] colsWidths) {
        //super(context, cellLayout, c, idsColumns, entityStatusColumn, from, to, 0);
        super(context, cursor, 0);
        mContext = context;
        mColumnsCount = columnsCount;
        mDataColumnsCount = dataColsCount;
        mColumnsWidths = colsWidths;
        mData = new FixedColumnsMatrix<T>(mDataColumnsCount);
    }

    public Context getContext() {
        return mContext;
    }

    public void setCellClickedListener(OnCellClickedListener listener) {
        mCellClickedListener = listener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    }

    protected void setColumnsWidths(int[] widths, boolean notifyChanged) {
        mColumnsWidths = widths;
        if (notifyChanged) {
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public View newRowView(ViewGroup parent) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(
                new AbsListView.LayoutParams(
                        AbsListView.LayoutParams.MATCH_PARENT,
                        AbsListView.LayoutParams.WRAP_CONTENT
                )
        );

        for (int column = 0; column < mColumnsCount; column++) {
            boolean isDataCol = isDataColumn(column);
            int cellWidth;
            if (isDataCol) {
                if (mColumnsWidths[column] == 0) {
                    cellWidth = parent.getWidth() / mColumnsCount - 1;
                } else {
                    cellWidth = mColumnsWidths[column];
                }
            } else {
                cellWidth = mColumnsWidths[column];
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    cellWidth,
                    LinearLayout.LayoutParams.MATCH_PARENT);

            View cellView = isDataCol ? newDataColumn(row) : newNonDataColumn(column);
            row.addView(cellView, column, params);
        }
        return row;
    }


    public void bindRowView(int position, View view) {
        if (!(view instanceof LinearLayout)) {
            return;
        }
        LinearLayout rowView = (LinearLayout)view;
        for (int i = 0; i < rowView.getChildCount(); i++) {
            final int row = position;
            final int column = i;
            View cellView = rowView.getChildAt(i);
            assert cellView != null;
            cellView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mCellClickedListener != null) {
                        mCellClickedListener.onCellClicked(row, column);
                    }
                }
            });
            if (isDataColumn(i)) {
                bindDataColumn(position, i, cellView);
            } else {
                bindNonDataColumn(position, i, cellView);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = newRowView(parent);
        } else {
            v = convertView;
        }
        bindRowView(position, v);
        return v;
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        mData.clearAll();
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
            c.moveToPosition(i);
            convertCursorItemToMatrixRow(c);
        }
        notifyDataSetChanged();
        return super.swapCursor(c);
    }

    protected abstract void convertCursorItemToMatrixRow(Cursor cursor);
    protected abstract boolean isDataColumn(int column);
    protected abstract View newNonDataColumn(int column);
    protected abstract View newDataColumn(ViewGroup parent);
    protected abstract void bindNonDataColumn(int row, int column, View cell);
    protected abstract void bindDataColumn(int row, int column, View cell);

    public interface OnCellClickedListener {
        public void onCellClicked(int row, int column);
    }
}
