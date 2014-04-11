package com.despectra.android.journal.Data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.DropBoxManager;
import android.util.SparseArray;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Dmitry on 01.04.14.
 */
public class ProviderUpdater {

    // insert new, update existing
    public static final int MODE_APPEND = 0;
    //insert new, update existing, delete non-existing
    public static final int MODE_REPLACE = 1;

    private Context mContext;
    private ContentResolver mResolver;
    private String mProviderUri;

    public ProviderUpdater(Context context, String providerUri) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        mProviderUri = providerUri;
    }

    public void updateTableWithJSONArray(int updatingMode,
                                         String table,
                                         JSONArray json,
                                         String[] from,
                                         String[] to,
                                         String primaryInJson,
                                         String primaryInTable) throws JSONException {
        String tableUri = mProviderUri + "/" + table;
        Map<Long, Void> localRows = new HashMap<Long, Void>();
        Map<Long, JSONObject> jsonRows = new HashMap<Long, JSONObject>();
        Cursor allRows = mResolver.query(Uri.parse(tableUri), new String[]{primaryInTable}, null, null, null);
        if (allRows.moveToFirst()) {
            do {
                localRows.put(allRows.getLong(0), null);
            } while(allRows.moveToNext());
        }

        for (int i = 0; i < json.length(); i++) {
            jsonRows.put(json.getJSONObject(i).getLong(primaryInJson), json.getJSONObject(i));
        }

        //update existing
        Iterator localRowsIt = localRows.entrySet().iterator();
        while(localRowsIt.hasNext()) {
            Map.Entry<Long, Void> row = (Map.Entry<Long, Void>)localRowsIt.next();
            long key = row.getKey();
            if (jsonRows.containsKey(key)) {
                updateSingleRow(tableUri, String.format("%s = %d", primaryInTable, key), jsonRows.get(key), from, to);
                localRowsIt.remove();
                jsonRows.remove(key);
            }
        }
        //insert new
        Iterator jsonRowsIt = jsonRows.entrySet().iterator();
        while(jsonRowsIt.hasNext()) {
            Map.Entry<Long, JSONObject> row = (Map.Entry<Long, JSONObject>)jsonRowsIt.next();
            insertNewRow(tableUri, row.getValue(), from, to);
            jsonRowsIt.remove();
        }
        if (updatingMode == MODE_REPLACE) {
            //delete non-existing
            for (Map.Entry<Long, Void> row : localRows.entrySet()) {
                long key = row.getKey();
                mResolver.delete(Uri.parse(tableUri), String.format("%s = %d", primaryInTable, key), null);
            }
        }
    }

    private void insertNewRow(String tableUri, JSONObject jsonRow, String[] from, String[] to) throws JSONException {
        ContentValues data = getContentValuesFromJson(jsonRow, from, to);
        mResolver.insert(Uri.parse(tableUri), data);
    }

    private void updateSingleRow(String tableUri, String where, JSONObject jsonData, String[] from, String[] to) throws JSONException {
        ContentValues data = getContentValuesFromJson(jsonData, from, to);
        mResolver.update(Uri.parse(tableUri), data, where, null);
    }

    private ContentValues getContentValuesFromJson(JSONObject json, String[] from, String[] to) throws JSONException {
        ContentValues data = new ContentValues();
        for (int i = 0; i < from.length; i++) {
            data.put(to[i], json.getString(from[i]));
        }
        return data;
    }

    public long insertTempRow(String table, ContentValues data) {
        data.put(Contract.FIELD_ENTITY_STATUS, Contract.STATUS_INSERTING);
        Uri result = mResolver.insert(
                Uri.parse(mProviderUri + "/" + table),
                data
        );
        return Long.valueOf(result.getLastPathSegment());
    }

    public void persistTempRow(String table, long localId, long remoteId) {
        ContentValues data = new ContentValues();
        data.put(Contract.FIELD_REMOTE_ID, remoteId);
        data.put(Contract.FIELD_ENTITY_STATUS, Contract.STATUS_IDLE);
        mResolver.update(
                Uri.parse(mProviderUri + "/" + table),
                data,
                "_id = " + localId,
                null
        );
    }

    public void markRowAsIdle(String table, long remoteId) {
        markRowEntityWithStatus(table, remoteId, Contract.STATUS_IDLE);
    }

    public void markRowAsUpdating(String table, long remoteId) {
        markRowEntityWithStatus(table, remoteId, Contract.STATUS_UPDATING);
    }

    public void markRowAsDeleting(String table, long remoteId) {
        markRowEntityWithStatus(table, remoteId, Contract.STATUS_DELETING);
    }

    private void markRowEntityWithStatus(String table, long remoteId, int status) {
        ContentValues data = new ContentValues();
        data.put(Contract.FIELD_ENTITY_STATUS, status);
        mResolver.update(
                Uri.parse(mProviderUri + "/" + table),
                data,
                Contract.FIELD_REMOTE_ID + " = " + remoteId,
                null
        );
    }

    public void deleteMarkedRows(String table) {
        mResolver.delete(
                Uri.parse(mProviderUri + "/" + table),
                Contract.FIELD_ENTITY_STATUS + " = " + Contract.STATUS_DELETING,
                null
        );
    }
}
