package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import com.despectra.android.journal.model.EntityIdsColumns;

/**
 * Created by Dmitry on 18.06.14.
 */
public abstract class PseudoGridAdapter extends RemoteIdsCursorAdapter {

    private int mColumnsCount;
    private int mDataColumnsCount;
    private int[] mColumnsWidths;


    public PseudoGridAdapter(Context context, Cursor c, EntityIdsColumns[] idsColumns, String entityStatusColumn,
                             int cellLayout, int columnsCount, int dataColsCount, int[] colsWidths, String[] from, int[] to) {
        super(context, cellLayout, c, idsColumns, entityStatusColumn, from, to, 0);
        mColumnsCount = columnsCount;
        mDataColumnsCount = dataColsCount;
        mColumnsWidths = colsWidths;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    /*protected void setColumnsWidths(int[] widths, boolean notifyChanged) {
        mColumnsWidths = widths;
        if (notifyChanged) {
            notifyDataSetChanged();
        }
    }

    public void setColumnWidthAtPos(int position, int width) {
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


    public View newRowView(Cursor cursor, ViewGroup parent) {
        LinearLayout row = new LinearLayout(getContext()) {
            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                return false;
            }
        };
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(
                new AbsListView.LayoutParams(
                        AbsListView.LayoutParams.MATCH_PARENT,
                        AbsListView.LayoutParams.WRAP_CONTENT
                )
        );
        int cellWidth = -1;
        for (int column = 0; column < mColumnsCount; column++) {
            if (mColumnsWidths[column] == 0) {
                if (cellWidth == -1) {
                    cellWidth = parent.getWidth() / mColumnsCount - 1;
                }
            } else {
                cellWidth = mColumnsWidths[column];
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    cellWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            row.addView(isDataColumn(column) ? super.newView(getContext(), cursor, parent) : newNonDataColumn(column), params);
        }
        return row;
    }


    public void bindRowView(int position, View view, Cursor cursor) {
        if (!(view instanceof LinearLayout)) {
            return;
        }
        LinearLayout row = (LinearLayout)view;
        for (int i = 0; i < row.getChildCount(); i++) {
            if (isDataColumn(i)) {
                bindDataColumn(position, row.getChildAt(i), cursor);
                cursor.moveToNext();
            } else {
                bindNonDataColumn(position, i, row.getChildAt(i), cursor);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = getCursor();
        if (!cursor.moveToPosition(position * mDataColumnsCount)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        View v;
        if (convertView == null) {
            v = newRowView(cursor, parent);
        } else {
            v = convertView;
        }
        bindRowView(position, v, cursor);
        return v;
    }

    protected abstract boolean isDataColumn(int column);
    protected abstract View newNonDataColumn(int column);
    protected abstract void bindNonDataColumn(int row, int column, View cell, Cursor cursor);
    protected abstract void bindDataColumn(int row, View cell, Cursor cursor);
}
