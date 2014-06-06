package com.despectra.android.journal.logic.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by Dmitry on 01.04.14.
 */
public class LocalStorageManager {

    // insert new, update existing
    public static final int MODE_APPEND = 0;
    //insert new, update existing, delete non-existing
    public static final int MODE_REPLACE = 1;

    private Context mContext;
    private ContentResolver mResolver;

    public LocalStorageManager(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
    }

    public ContentResolver getResolver() {
        return mResolver;
    }

    public void notifyUri(Uri uri) {
        getResolver().notifyChange(uri, null);
    }

    public void notifyUriForClients(Uri uri, ApiAction executingAction, String... clientsNames) {
        boolean notify = false;
        for (String client : clientsNames) {
            notify = notify || executingAction.clientTag.equals(client);
            if (notify) {
                break;
            }
        }
        if (notify) {
            notifyUri(uri);
        }
    }

    public Map<Long, Long> updateEntityWithJSONArray(int updatingMode,
                                            Cursor existingIds,
                                            Contract.EntityTable table,
                                            JSONArray json,
                                            String jsonIdCol,
                                            String[] jsonCols,
                                            String[] entityTableCols) throws JSONException {
        Map<Long, Long> existingRows = new HashMap<Long, Long>();
        Map<Long, JSONObject> receivedRows = new HashMap<Long, JSONObject>();
        Map<Long, Long> affectedRows = new HashMap<Long, Long>();

        int remoteIdColIndex = existingIds.getColumnIndex(table.REMOTE_ID);
        int localIdColIndex = existingIds.getColumnIndex(table._ID);

        if (existingIds.moveToFirst()) {
            do {
                existingRows.put(existingIds.getLong(localIdColIndex), existingIds.getLong(remoteIdColIndex));
            } while(existingIds.moveToNext());
        }

        for (int i = 0; i < json.length(); i++) {
            receivedRows.put(json.getJSONObject(i).getLong(jsonIdCol), json.getJSONObject(i));
        }

        //update existing
        Iterator existingRowsIt = existingRows.entrySet().iterator();
        while(existingRowsIt.hasNext()) {
            Map.Entry<Long, Long> row = (Map.Entry<Long, Long>)existingRowsIt.next();
            long localId = row.getKey();
            long remoteId = row.getValue();
            if (receivedRows.containsKey(remoteId)) {
                updateSingleEntity(table, localId, receivedRows.get(remoteId), jsonCols, entityTableCols);
                affectedRows.put(remoteId, localId);
                existingRowsIt.remove();
                receivedRows.remove(remoteId);
            }
        }

        //insert new
        Iterator jsonRowsIt = receivedRows.entrySet().iterator();
        while(jsonRowsIt.hasNext()) {
            Map.Entry<Long, JSONObject> row = (Map.Entry<Long, JSONObject>)jsonRowsIt.next();
            Map.Entry<Long, Long> insertedIds = insertNewEntity(table, row.getValue(), row.getKey(), jsonCols, entityTableCols);
            affectedRows.put(insertedIds.getKey(), insertedIds.getValue());
            jsonRowsIt.remove();
        }

        if (updatingMode == MODE_REPLACE) {
            //delete non-existing
            for (Map.Entry<Long, Long> row : existingRows.entrySet()) {
                long localId = row.getKey();
                deleteEntityByLocalId(table, localId);
            }
        }
        return affectedRows;
    }

    public Map.Entry<Long, Long> insertNewEntity(Contract.EntityTable table, JSONObject jsonDataRow, long insertingId, String[] from, String[] to) throws JSONException {
        ContentValues data = getContentValuesFromJson(jsonDataRow, from, to);
        data.put(table.REMOTE_ID, insertingId);
        Uri result = mResolver.insert(table.URI, data);
        long localInsertedId = Long.valueOf(result.getLastPathSegment());
        return new AbstractMap.SimpleEntry<Long, Long>(insertingId, localInsertedId);
    }

    public void updateSingleEntity(Contract.EntityTable localTable, long localId, JSONObject jsonData, String[] from, String[] to) throws JSONException {
        ContentValues data = getContentValuesFromJson(jsonData, from, to);
        mResolver.update(localTable.URI, data, String.format("%s = %d", localTable._ID, localId), null);
    }

    private ContentValues getContentValuesFromJson(JSONObject json, String[] from, String[] to) throws JSONException {
        ContentValues data = new ContentValues();
        for (int i = 0; i < from.length; i++) {
            data.put(to[i], json.getString(from[i]));
        }
        return data;
    }

    public long insertTempRow(Contract.EntityTable localTable, ContentValues data) {
        data.put(localTable.ENTITY_STATUS, Contract.STATUS_INSERTING);
        Uri result = mResolver.insert(localTable.URI, data);
        return Long.valueOf(result.getLastPathSegment());
    }

    public void persistTempRow(Contract.EntityTable table,
                               long localId,
                               long remoteId) {
        ContentValues data = new ContentValues();
        data.put(table.ENTITY_STATUS, Contract.STATUS_IDLE);
        data.put(table.REMOTE_ID, remoteId);
        int affected = mResolver.update(
                table.URI,
                data,
                table._ID + " = ?",
                new String[]{String.valueOf(localId)}
        );
    }

    public void persistUpdatingRow(Contract.EntityTable table, String localId, ContentValues data) {
        data.put(table.ENTITY_STATUS, Contract.STATUS_IDLE);
        mResolver.update(table.URI, data, table._ID + " = ?", new String[]{localId});
    }

    public void deleteMarkedEntities(Contract.EntityTable table) {
        mResolver.delete(
                table.URI,
                table.ENTITY_STATUS + " = ?",
                new String[]{String.valueOf(Contract.STATUS_DELETING)}
        );
    }

    public void deleteEntityByLocalId(Contract.EntityTable localTable, long localId) {
        String[] args = new String[]{String.valueOf(localId)};
        mResolver.delete(localTable.URI, localTable._ID + " = ?", args);
    }

    public long getRemoteIdByLocal(Contract.EntityTable table, long localId) {
        Cursor row = mResolver.query(
                table.URI,
                new String[]{table.REMOTE_ID},
                table._ID + " = ?",
                new String[]{String.valueOf(localId)},
                null
        );
        if (row.getCount() > 0) {
            row.moveToFirst();
            return row.getLong(0);
        }
        return -1;
    }

    public long getLocalIdByRemote(Contract.EntityTable table, long remoteId) {
        Cursor row = mResolver.query(
                table.URI,
                new String[]{table._ID},
                table.REMOTE_ID + " = ?",
                new String[]{String.valueOf(remoteId)},
                null
        );
        if (row.getCount() > 0) {
            row.moveToFirst();
            return row.getLong(0);
        }
        return -1;
    }

    public void markRowAsIdle(Contract.EntityTable table, String localId) {
        markEntityRowWithStatus(table, localId, Contract.STATUS_IDLE);
    }

    public void markRowAsUpdating(Contract.EntityTable table, String localId) {
        markEntityRowWithStatus(table, localId, Contract.STATUS_UPDATING);
    }

    public void markRowAsDeleting(Contract.EntityTable table, String localId) {
        markEntityRowWithStatus(table, localId, Contract.STATUS_DELETING);
    }

    private void markEntityRowWithStatus(Contract.EntityTable table, String localId, int status) {
        ContentValues data = new ContentValues();
        data.put(table.ENTITY_STATUS, status);
        mResolver.update(table.URI,  data, table._ID + " = ?", new String[]{localId});
    }
}
