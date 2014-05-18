package com.despectra.android.journal.logic.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.despectra.android.journal.logic.local.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

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

    public ContentResolver getResolver() {
        return mResolver;
    }

    public Map<String, String> updateEntityWithJSONArray(int updatingMode,
                                            Cursor existingIds,
                                            Contract.EntityColumnsHolder localTable,
                                            Contract.RemoteColumnsHolder remoteTable,
                                            JSONArray json,
                                            String jsonIdCol,
                                            String[] jsonCols,
                                            String[] entityTableCols) throws JSONException {
        Map<Long, Long> existingRows = new HashMap<Long, Long>();
        Map<Long, JSONObject> receivedRows = new HashMap<Long, JSONObject>();
        Map<String, String> insertedRows = new HashMap<String, String>();

        int remoteIdColIndex = existingIds.getColumnIndex(remoteTable.REMOTE_ID);
        int localIdColIndex = existingIds.getColumnIndex(remoteTable._ID);

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
                updateSingleEntity(localTable, localId, receivedRows.get(remoteId), jsonCols, entityTableCols);
                existingRowsIt.remove();
                receivedRows.remove(remoteId);
            }
        }

        //insert new
        Iterator jsonRowsIt = receivedRows.entrySet().iterator();
        while(jsonRowsIt.hasNext()) {
            Map.Entry<Long, JSONObject> row = (Map.Entry<Long, JSONObject>)jsonRowsIt.next();
            Map.Entry<String, String> insertedIds = insertNewEntity(localTable, remoteTable, row.getValue(), jsonIdCol, jsonCols, entityTableCols);
            insertedRows.put(insertedIds.getKey(), insertedIds.getValue());
            jsonRowsIt.remove();
        }

        if (updatingMode == MODE_REPLACE) {
            //delete non-existing
            for (Map.Entry<Long, Long> row : existingRows.entrySet()) {
                long localId = row.getKey();
                mResolver.delete(Uri.parse(mProviderUri + "/" + localTable.TABLE) , String.format("%s = %d", localTable._ID, localId), null);
                mResolver.delete(Uri.parse(mProviderUri + "/" + remoteTable.TABLE), String.format("%s = %d", remoteTable._ID, localId), null);
            }
        }
        return insertedRows;
    }

    private Map.Entry<String, String> insertNewEntity(Contract.EntityColumnsHolder localTable, Contract.RemoteColumnsHolder remoteTable, JSONObject jsonRow, String jsonIdCol, String[] from, String[] to) throws JSONException {
        ContentValues data = getContentValuesFromJson(jsonRow, from, to);
        Uri result = mResolver.insert(Uri.parse(mProviderUri + "/" + localTable.TABLE), data);
        String localId = result.getLastPathSegment();
        String remoteId = jsonRow.getString(jsonIdCol);
        ContentValues remoteIdData = new ContentValues();
        remoteIdData.put(remoteTable._ID, localId);
        remoteIdData.put(remoteTable.REMOTE_ID, remoteId);
        mResolver.insert(Uri.parse(mProviderUri + "/" + remoteTable.TABLE), remoteIdData);
        return new AbstractMap.SimpleEntry<String, String>(remoteId, localId);
    }

    private void updateSingleEntity(Contract.EntityColumnsHolder localTable, long localId, JSONObject jsonData, String[] from, String[] to) throws JSONException {
        ContentValues data = getContentValuesFromJson(jsonData, from, to);
        mResolver.update(Uri.parse(mProviderUri + "/" + localTable.TABLE), data, String.format("%s = %d", localTable._ID, localId), null);
    }

    private ContentValues getContentValuesFromJson(JSONObject json, String[] from, String[] to) throws JSONException {
        ContentValues data = new ContentValues();
        for (int i = 0; i < from.length; i++) {
            data.put(to[i], json.getString(from[i]));
        }
        return data;
    }

    public long insertTempRow(Contract.EntityColumnsHolder localTable, Contract.RemoteColumnsHolder remoteTable, ContentValues data) {
        data.put(localTable.ENTITY_STATUS, Contract.STATUS_INSERTING);
        Uri result = mResolver.insert(Uri.parse(mProviderUri + "/" + localTable.TABLE), data);
        long localId = Long.valueOf(result.getLastPathSegment());

        ContentValues idsData = new ContentValues();
        idsData.put(remoteTable._ID, localId);
        mResolver.insert(Uri.parse(mProviderUri + "/" + remoteTable.TABLE), idsData);
        return localId;
    }

    public void persistTempRow(Contract.EntityColumnsHolder localTable,
                               Contract.RemoteColumnsHolder remoteTable,
                               long localId,
                               long remoteId) {
        ContentValues data = new ContentValues();
        data.put(localTable.ENTITY_STATUS, Contract.STATUS_IDLE);
        mResolver.update(
                Uri.parse(mProviderUri + "/" + localTable.TABLE),
                data,
                String.format("%s = %d", localTable._ID, localId),
                null
        );

        ContentValues idsData = new ContentValues();
        idsData.put(remoteTable.REMOTE_ID, remoteId);
        mResolver.update(
                Uri.parse(mProviderUri + "/" + remoteTable.TABLE),
                idsData,
                String.format("%s = %d", remoteTable._ID, localId),
                null
        );
    }

    public void persistUpdatingRow(Contract.EntityColumnsHolder table, String localId, ContentValues data) {
        data.put(table.ENTITY_STATUS, Contract.STATUS_IDLE);
        mResolver.update(
                Uri.parse(mProviderUri + "/" + table.TABLE),
                data,
                table._ID + " = ?",
                new String[]{localId}
        );
    }

    public void deleteMarkedEntities(Contract.EntityColumnsHolder local, Contract.RemoteColumnsHolder remote) {
        Cursor localDeletingIds = mResolver.query(
                Uri.parse(mProviderUri + "/" + local.TABLE),
                new String[]{local._ID},
                local.ENTITY_STATUS + " = ?",
                new String[]{String.valueOf(Contract.STATUS_DELETING)},
                null);
        deleteMarkedRows(local);
        localDeletingIds.moveToFirst();
        do {
            mResolver.delete(
                    Uri.parse(mProviderUri + "/" + remote.TABLE),
                    remote._ID + " = ?",
                    new String[]{localDeletingIds.getString(0)}
            );
        } while (localDeletingIds.moveToNext());
    }

    public void deleteMarkedRows(Contract.EntityColumnsHolder table) {
        mResolver.delete(
                Uri.parse(mProviderUri + "/" + table.TABLE),
                table.ENTITY_STATUS + " = " + Contract.STATUS_DELETING,
                null
        );
    }


    public void markRowAsIdle(Contract.EntityColumnsHolder table, String localId) {
        markEntityRowWithStatus(table, localId, Contract.STATUS_IDLE);
    }

    public void markRowAsUpdating(Contract.EntityColumnsHolder table, String localId) {
        markEntityRowWithStatus(table, localId, Contract.STATUS_UPDATING);
    }

    public void markRowAsDeleting(Contract.EntityColumnsHolder table, String localId) {
        markEntityRowWithStatus(table, localId, Contract.STATUS_DELETING);
    }

    private void markEntityRowWithStatus(Contract.EntityColumnsHolder table, String localId, int status) {
        ContentValues data = new ContentValues();
        data.put(table.ENTITY_STATUS, status);
        mResolver.update(
                Uri.parse(mProviderUri + "/" + table.TABLE),
                data,
                table._ID + " = ?",
                new String[]{localId}
        );
    }
}
