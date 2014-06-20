package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Matrix;
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
public abstract class PseudoGridAdapter<T> extends BaseAdapter {

    private Context mContext;
    private int mColumnsCount;
    private int mDataColumnsCount;
    private int[] mColumnsWidths;
    protected FixedColumnsMatrix<T> mData;

    public PseudoGridAdapter(Context context, int columnsCount, int dataColsCount, int[] colsWidths) {
        //super(context, cellLayout, c, idsColumns, entityStatusColumn, from, to, 0);
        super();
        mContext = context;
        mColumnsCount = columnsCount;
        mDataColumnsCount = dataColsCount;
        mColumnsWidths = colsWidths;
        mData = new FixedColumnsMatrix<T>(mDataColumnsCount);
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    protected void setColumnsWidths(int[] widths, boolean notifyChanged) {
        mColumnsWidths = widths;
        if (notifyChanged) {
            notifyDataSetChanged();
        }
    }

    /*public void setColumnWidthAtPos(int position, int width) {
        mColumnsWidths[position] = width;
        notifyDataSetChanged();
    }

    protected void setCellLayoutAtPos(int position, int layout, boolean notifyChanged) {
        mCellsLayouts[position] = layout;
        if (notifyChanged) {
            notifyDataSetChanged();
        }
    }*/

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

            row.addView(isDataCol ? newDataColumn(row) : newNonDataColumn(column), column, params);
        }
        return row;
    }


    public void bindRowView(int position, View view) {
        if (!(view instanceof LinearLayout)) {
            return;
        }
        LinearLayout row = (LinearLayout)view;
        for (int i = 0; i < row.getChildCount(); i++) {
            if (isDataColumn(i)) {
                bindDataColumn(position, i, row.getChildAt(i));
            } else {
                bindNonDataColumn(position, i, row.getChildAt(i));
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

    public void swapCursor(Cursor c) {
        mData.clearAll();
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
            c.moveToPosition(i);
            convertCursorItemToMatrixRow(c);
        }
        notifyDataSetChanged();
        if (!c.isClosed()) {
            c.close();
        }
    }

    protected abstract void convertCursorItemToMatrixRow(Cursor cursor);
    protected abstract boolean isDataColumn(int column);
    protected abstract View newNonDataColumn(int column);
    protected abstract View newDataColumn(ViewGroup parent);
    protected abstract void bindNonDataColumn(int row, int column, View cell);
    protected abstract void bindDataColumn(int row, int column, View cell);
}
