package com.despectra.android.journal.model;

import android.util.LongSparseArray;

/**
 * Created by Dmitry on 19.06.14.
 */
public class FixedColumnsMatrix<T> {
    private LongSparseArray<LongSparseArray<T>> mContainer;
    private int mColumnsCount;
    private long mMaxRowIndex;

    public FixedColumnsMatrix(int columnsCount) {
        mColumnsCount = columnsCount;
        mContainer = new LongSparseArray<LongSparseArray<T>>();
        mMaxRowIndex = -1;
    }

    public void putCell(long row, long column, T what) {
        if (mContainer.get(row) == null) {
            mContainer.put(row, new LongSparseArray<T>(mColumnsCount));
            if (mMaxRowIndex < row) {
                mMaxRowIndex = row;
            }
        }
        LongSparseArray<T> rowContainer = mContainer.get(row);
        rowContainer.put(column, what);
    }

    public void clearAll() {
        mContainer.clear();
        mMaxRowIndex = -1;
    }

    public T get(long row, long column) {
        LongSparseArray<T> rowContainer = mContainer.get(row);
        if (rowContainer != null) {
            return rowContainer.get(column);
        }
        return null;
    }

    public long rowsCount() {
        return mMaxRowIndex + 1;
    }
}
