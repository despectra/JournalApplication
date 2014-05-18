package com.despectra.android.journal.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

/**
 * Created by Dmirty on 04.03.14.
 */
public abstract class HorizontalViewsRowAdapter extends BaseAdapter {

    private int mCellLayout;
    private int mColumnsCount;
    private Object[] mData;
    private Context mContext;
    private int[] mColumnsWidths;
    private int[] mCellsLayouts;

    private OnItemClickListener mListener;

    public HorizontalViewsRowAdapter(Context context, int cellLayout, int columnsCount, Object[] data) {
        int[] layouts = new int[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            layouts[i] = cellLayout;
        }
        init(context, layouts, columnsCount, null, data);
    }

    public HorizontalViewsRowAdapter(Context c, int[] cellsLayouts, int columnsCount, Object[] data) {
        init(c, cellsLayouts, columnsCount, null, data);
    }

    public HorizontalViewsRowAdapter(Context c, int[] cellsLayouts, int columnsCount, int[] columnsWidths, Object[] data) {
        init(c, cellsLayouts, columnsCount, columnsWidths, data);
    }

    private void init(Context context, int[] cellsLayouts, int columnsCount, int[] columnsWidths, Object[] data) {
        if (data != null && data.length % columnsCount != 0) {
            throw new IllegalArgumentException("Data items count must be KRATNYM to columns count");
        }
        mContext = context;
        mCellsLayouts = cellsLayouts;
        mColumnsCount = columnsCount;
        mData = data;
        if (cellsLayouts == null) {
            mCellsLayouts = new int[mColumnsCount];
        } else {
            mCellsLayouts = cellsLayouts;
        }
        if (columnsWidths == null) {
            mColumnsWidths = new int[mColumnsCount];
        } else {
            mColumnsWidths = columnsWidths;
        }

    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return mData.length / mColumnsCount;
    }

    @Override
    public Object getItem(int i) {
        return mData[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    protected void setData(Object[] data, boolean notifyChanged) {
        mData = data;
        if (notifyChanged) {
            notifyDataSetChanged();
        }
    }

    protected void setColumnsWidths(int[] widths, boolean notifyChanged) {
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
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        LinearLayout row;
        if(v == null) {
            row = new LinearLayout(mContext) {
                @Override
                public boolean onTouchEvent(MotionEvent ev) {
                    return false;
                }

                @Override
                public boolean onInterceptTouchEvent(MotionEvent ev) {
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
                View cellView = LayoutInflater.from(mContext).inflate(mCellsLayouts[column], null, false);
                defineNewViewHolder(position, column, cellView);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        cellWidth,
                        ViewGroup.LayoutParams.MATCH_PARENT);

                row.addView(cellView, params);
            }
            v = row;
        }
        row = (LinearLayout)v;
        for (int column = 0; column < mColumnsCount; column++) {
            bindSingleViewToData(position, column, row.getChildAt(column), mData[position * mColumnsCount + column]);
            final int col = column;
            row.getChildAt(column).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onItemClick(position, mData[position * mColumnsCount + col]);
                    }
                }
            });
        }
        return v;
    }

    public abstract void defineNewViewHolder(int row, int column, View view);
    public abstract void bindSingleViewToData(int row, int column, View view, Object dataObject);

    public interface OnItemClickListener {
        public void onItemClick(int position, Object itemData);
    }
}
