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
        check(row, column);
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
        check(row, column);
        LongSparseArray<T> rowContainer = mContainer.get(row);
        if (rowContainer != null) {
            return rowContainer.get(column);
        }
        return null;
    }

    private void check(long row, long column) throws IllegalArgumentException {
        if (row < 0) {
            throw new IllegalArgumentException("Wrong row index");
        }
        if (column < 0 || column >= mColumnsCount) {
            throw new IllegalArgumentException("Wrong column index");
        }
    }

    public long rowsCount() {
        return mMaxRowIndex + 1;
    }
}
