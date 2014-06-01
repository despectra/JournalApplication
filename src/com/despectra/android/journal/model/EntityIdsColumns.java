package com.despectra.android.journal.model;

import android.database.Cursor;

/**
 * Created by Dmitry on 01.06.14.
 */
public class EntityIdsColumns {

    private String tableName;
    private String localIdColumn;
    private String remoteIdColumn;
    private int localIdColumnIndex;
    private int remoteIdColumnIndex;

    public EntityIdsColumns(String tableName, String localIdColumn, String remoteIdColumn) {
        this.tableName = tableName;
        this.localIdColumn = localIdColumn;
        this.remoteIdColumn = remoteIdColumn;
    }

    public String getTableName() {
        return tableName;
    }

    public String getLocalIdColumn() {
        return localIdColumn;
    }

    public String getRemoteIdColumn() {
        return remoteIdColumn;
    }

    public int getLocalIdColumnIndex() {
        return localIdColumnIndex;
    }

    public int getRemoteIdColumnIndex() {
        return remoteIdColumnIndex;
    }

    public void updateColumnsIndexes(Cursor cursor) {
        localIdColumnIndex = cursor.getColumnIndexOrThrow(localIdColumn);
        remoteIdColumnIndex = cursor.getColumnIndexOrThrow(remoteIdColumn);
    }
}
