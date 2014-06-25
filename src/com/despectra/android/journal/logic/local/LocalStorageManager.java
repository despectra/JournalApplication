package com.despectra.android.journal.logic.local;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.LongSparseArray;
import android.util.Pair;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.utils.JSONBuilder;
import com.despectra.android.journal.utils.Utils;
import com.google.common.base.Function;
import com.google.common.primitives.Longs;
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
    private PostCallbacks mPostCallbacks;

    public LocalStorageManager(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
    }

    public ContentResolver getResolver() {
        return mResolver;
    }

    public void setCallbacks(PostCallbacks callbacks) {
        mPostCallbacks = callbacks;
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

    public void updateComplexEntityWithJsonResponse(int updatingMode,
                                                    Cursor existingInitIds,
                                                    EntityTable initTable,
                                                    JSONArray jsonData,
                                                    PreCallbacks preCallbacks)
            throws JSONException,
            OperationApplicationException,
            RemoteException {
        LongSparseArray<Long> existingInitRows = new LongSparseArray<Long>();
        LongSparseArray<JSONObject> receivedRows = new LongSparseArray<JSONObject>();

        int remoteIdColIndex = existingInitIds.getColumnIndex(initTable.REMOTE_ID);
        int localIdColIndex = existingInitIds.getColumnIndex(initTable._ID);

        if (existingInitIds.moveToFirst()) {
            do {
                existingInitRows.put(existingInitIds.getLong(localIdColIndex), existingInitIds.getLong(remoteIdColIndex));
            } while (existingInitIds.moveToNext());
        }

        for (int i = 0; i < jsonData.length(); i++) {
            JSONObject row = jsonData.getJSONObject(i);
            receivedRows.put(row.getLong(initTable.DATA_FIELDS.get(initTable.REMOTE_ID)), row);
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        //update existing
        LongSparseArray<Long> existingInitRowsCopy = existingInitRows.clone();
        for (int i = 0; i < existingInitRows.size(); i++) {
            long localId = existingInitRows.keyAt(i);
            long remoteId = existingInitRows.valueAt(i);
            JSONObject row = receivedRows.get(remoteId);
            if (row != null) {
                prepareEntityToUpdating(initTable,
                        localId,
                        row,
                        batch,
                        false,
                        preCallbacks);
                existingInitRowsCopy.remove(localId);
                receivedRows.remove(remoteId);
            }
        }
        runBatch(batch);

        //insert new
        int backRefIndex = -1;
        for (int i = 0; i < receivedRows.size(); i++) {
            JSONObject row = receivedRows.valueAt(i);
            prepareEntityToInsertion(initTable,
                    row,
                    batch,
                    backRefIndex,
                    false,
                    preCallbacks);
        }
        runBatch(batch);

        if (updatingMode == MODE_REPLACE) {
            //delete non-existing
            for (int i = 0; i < existingInitRowsCopy.size(); i++) {
                long localId = existingInitRowsCopy.keyAt(i);
                prepareEntityForDeletionCascade(initTable,
                        new long[]{localId},
                        batch,
                        false,
                        preCallbacks);
            }
        }
        runBatch(batch);
    }

    public long preInsertEntity(EntityTable initTable, JSONObject insertingData) throws Exception {
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        prepareEntityToInsertion(initTable, insertingData, batch, 0, true, null);
        ContentProviderResult[] result = runBatch(batch);
        if (result != null && result.length > 0) {
            return ContentUris.parseId(result[0].uri);
        }
        return -1;
    }

    public void markEntityAsIdle(EntityTable initTable, long initLocalId) throws Exception {
        markEntityWithStatus(initTable, initLocalId, Contract.STATUS_IDLE);
    }

    public void markEntityAsUpdating(EntityTable initTable, long initLocalId) throws Exception {
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        prepareEntityToUpdating(initTable, initLocalId, null, batch, true, null);
        runBatch(batch);
    }

    public void markEntityAsDeletingCascade(EntityTable initTable, long initLocalId) throws Exception {
        markEntitiesAdDeletingCascasde(initTable, new long[]{initLocalId});
    }

    public void markEntitiesAdDeletingCascasde(EntityTable initTable, long[] initLocalIds) throws Exception {
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        prepareEntityForDeletionCascade(initTable, initLocalIds, batch, true, null);
        runBatch(batch);
    }

    private void markEntityWithStatus(EntityTable initTable, long initLocalId, int status) throws Exception {
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        prepareEntityToUpdating(initTable, initLocalId,
                new JSONBuilder().addKeyValue("entity_status", status).create(), batch, false, null);
        runBatch(batch);
    }

    private ContentProviderResult[] runBatch(ArrayList<ContentProviderOperation> batch) throws OperationApplicationException, RemoteException {
        ContentProviderResult[] results;
        if (!batch.isEmpty()) {
            results = getResolver().applyBatch(Contract.AUTHORITY, batch);
            batch.clear();
            return results;
        }
        return null;
    }

    private void prepareEntityToInsertion(EntityTable currentTable,
                                          JSONObject insertingDataRow,
                                          List<ContentProviderOperation> operations,
                                          int backRefIndex,
                                          boolean markAsInserting,
                                          PreCallbacks preCallbacks) throws JSONException {
        ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newInsert(currentTable.URI);
        Pair<EntityTable, String> dependency = currentTable.BACK_DEPENDENCY;
        boolean hasBackDependency = dependency != null;
        if (hasBackDependency) {
            prepareEntityToInsertion(
                    dependency.first, insertingDataRow, operations, backRefIndex, markAsInserting, preCallbacks
            );
            opBuilder.withValueBackReference(dependency.second, backRefIndex);
        }
        backRefIndex++;
        ContentValues values = new ContentValues();
        if (markAsInserting) {
            values.put(currentTable.ENTITY_STATUS, Contract.STATUS_INSERTING);
        }

        Iterator<String> dataRowIterator = insertingDataRow.keys();
        while (dataRowIterator.hasNext()) {
            String jsonKey = dataRowIterator.next();
            String tableColumn = currentTable.DATA_FIELDS.inverse().get(jsonKey);
            if (hasBackDependency) {
                if (!dependency.second.equals(tableColumn)) {
                    values.put(tableColumn, insertingDataRow.getString(jsonKey));
                }
            } else {
                values.put(tableColumn, insertingDataRow.getString(jsonKey));
            }
        }

        if(preCallbacks != null) {
            if(preCallbacks.onPreInsert(currentTable, values)) {
                operations.add(opBuilder.withValues(values).build());
            }
        } else {
            operations.add(opBuilder.withValues(values).build());
        }
    }

    private void prepareEntityToUpdating(EntityTable currentTable,
                                         long currentLocalId,
                                         JSONObject receivedRow,
                                         List<ContentProviderOperation> operations,
                                         boolean justMark,
                                         PreCallbacks preCallbacks) throws JSONException {
        ContentProviderOperation.Builder opBuilder = ContentProviderOperation.newUpdate(currentTable.URI);
        opBuilder.withSelection(currentTable._ID + " = ?", new String[]{String.valueOf(currentLocalId)});
        Pair<EntityTable, String> dependency = currentTable.BACK_DEPENDENCY;
        boolean hasBackDependency = dependency != null;
        if (hasBackDependency) {
            Cursor dependencyId = getResolver().query(currentTable.URI,
                    new String[]{dependency.second},
                    currentTable._ID + " = ?",
                    new String[]{String.valueOf(currentLocalId)},
                    null
            );
            if (dependencyId != null && dependencyId.getCount() > 0) {
                dependencyId.moveToFirst();
                prepareEntityToUpdating(dependency.first, dependencyId.getLong(0), receivedRow, operations, justMark, preCallbacks);
                opBuilder.withValue(dependency.second, dependencyId.getLong(0));
            }
        }
        ContentValues values = new ContentValues();
        if (justMark) {
            values.put(currentTable.ENTITY_STATUS, Contract.STATUS_UPDATING);
        } else {
            Iterator<String> dataRowIterator = receivedRow.keys();
            while (dataRowIterator.hasNext()) {
                String jsonKey = dataRowIterator.next();
                String tableColumn = currentTable.DATA_FIELDS.inverse().get(jsonKey);
                if (hasBackDependency) {
                    if (!dependency.second.equals(tableColumn)) {
                        values.put(tableColumn, receivedRow.getString(jsonKey));
                    }
                } else {
                    values.put(tableColumn, receivedRow.getString(jsonKey));
                }
            }
        }
        if (preCallbacks != null) {
            if (preCallbacks.onPreUpdate(currentTable, currentLocalId, values)) {
                operations.add(opBuilder.withValues(values).build());
            }
        } else {
            operations.add(opBuilder.withValues(values).build());
        }
    }

    public void prepareEntityForDeletionCascade(EntityTable currentTable,
                                                long[] currentLocalIds,
                                                final List<ContentProviderOperation> operations,
                                                final boolean justMark,
                                                final PreCallbacks preCallbacks) {
        Function<Pair<EntityTable, Long>, Void> callback = new Function<Pair<EntityTable, Long>, Void>() {
            @Override
            public Void apply(Pair<EntityTable, Long> args) {
                EntityTable table = args.first;
                long localId = args.second;
                ContentProviderOperation.Builder opBuilder = justMark
                        ? ContentProviderOperation.newUpdate(table.URI).withValue(table.ENTITY_STATUS, Contract.STATUS_DELETING)
                        : ContentProviderOperation.newDelete(table.URI)
                        .withSelection(table._ID + " = ?", new String[]{String.valueOf(localId)});
                if (preCallbacks != null) {
                    if(preCallbacks.onPreDelete(table, localId)) {
                        operations.add(opBuilder.build());
                    }
                } else {
                    operations.add(opBuilder.build());
                }
                return null;
            }
        };
        walkThroughAllDependencies(currentTable, currentLocalIds, new HashSet<EntityTable>(), callback);
    }

    private void walkThroughAllDependencies(EntityTable currentTable,
                                            long[] currentLocalIds,
                                            Set<EntityTable> visitedTables,
                                            Function<Pair<EntityTable, Long>, Void> endpointCallback) {
        if (visitedTables.contains(currentTable)) {
            return;
        }
        if (currentTable.BACK_DEPENDENCY != null) {
            long[] backDependencyIds = collectIdsForBackDependency(currentTable, currentLocalIds);
            visitedTables.add(currentTable);
            if (backDependencyIds.length > 0) {
                walkThroughAllDependencies(currentTable.BACK_DEPENDENCY.first, backDependencyIds, visitedTables, endpointCallback);
            }
        }
        if (currentTable.DIRECT_DEPENDENCIES.size() > 0) {
            for (Map.Entry<EntityTable, String> dependency : currentTable.DIRECT_DEPENDENCIES.entrySet()) {
                long[] directDependencyIds = collectIdsFromDirectDependency(dependency, currentLocalIds);
                visitedTables.add(currentTable);
                if (directDependencyIds.length > 0) {
                    walkThroughAllDependencies(dependency.getKey(), directDependencyIds, visitedTables, endpointCallback);
                }
            }
        }
        for (int i = 0; i < currentLocalIds.length; i++) {
            if (endpointCallback != null) {
                endpointCallback.apply(new Pair<EntityTable, Long>(currentTable, currentLocalIds[i]));
            }
        }
    }

    private long[] collectIdsFromDirectDependency(Map.Entry<EntityTable, String> dependency, long[] currentLocalIds) {
        long[] resultIds = new long[0];
        EntityTable depTable = dependency.getKey();
        String foreignKey = dependency.getValue();
        for (long currentLocalId : currentLocalIds) {
            Cursor depId = getResolver().query(depTable.URI,
                    new String[]{depTable._ID},
                    foreignKey + " = ?",
                    new String[]{String.valueOf(currentLocalId)},
                    null);
            if (depId.getCount() > 0) {
                resultIds = Longs.concat(resultIds, extractIdsFromIdsCursor(depId));
            }
        }
        return resultIds;
    }

    private long[] collectIdsForBackDependency(EntityTable targetTable, long[] localIds) {
        long[] resultIds = new long[0];
        String myForeignKey = targetTable.BACK_DEPENDENCY.second;
        for (long localId : localIds) {
            Cursor depId = getResolver().query(targetTable.URI,
                    new String[]{myForeignKey},
                    targetTable._ID + " = ?",
                    new String[]{String.valueOf(localId)},
                    null);
            if (depId.getCount() > 0) {
                resultIds = Longs.concat(resultIds, extractIdsFromIdsCursor(depId));
            }
        }
        return resultIds;
    }

    private long[] extractIdsFromIdsCursor(Cursor cursor) {
        long[] ids = new long[cursor.getCount()];
        int i = 0;
        do {
            ids[i] = cursor.getLong(0);
            i++;
        } while (cursor.moveToNext());
        return ids;
    }

    public Map<Long, Long> updateEntityWithJSONArray(int updatingMode,
                                Cursor existingIds,
                                Contract.EntityTable table,
                                JSONArray json,
                                String jsonIdCol,
                                String[] jsonCols,
                                String[] entityTableCols,
                                PreCallbacks checkingCallbacks) throws JSONException {
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
                updateSingleEntity(table, localId, receivedRows.get(remoteId), jsonCols, entityTableCols, checkingCallbacks);
                if (mPostCallbacks != null) {
                    mPostCallbacks.onUpdated(localId);
                }
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
            if (mPostCallbacks != null) {
                mPostCallbacks.onInserted(insertedIds.getValue());
            }
            affectedRows.put(insertedIds.getKey(), insertedIds.getValue());
            jsonRowsIt.remove();
        }

        if (updatingMode == MODE_REPLACE) {
            //delete non-existing
            for (Map.Entry<Long, Long> row : existingRows.entrySet()) {
                long localId = row.getKey();
                deleteEntityByLocalId(table, localId);
                if (mPostCallbacks != null) {
                    mPostCallbacks.onDeleted(localId);
                }
            }
        }
        return affectedRows;
    }

    public Map<Long, Long> updateEntityWithJSONArray(int updatingMode,
                                            Cursor existingIds,
                                            Contract.EntityTable table,
                                            JSONArray json,
                                            String jsonIdCol,
                                            String[] jsonCols,
                                            String[] entityTableCols) throws JSONException {
        return updateEntityWithJSONArray(updatingMode, existingIds, table, json, jsonIdCol, jsonCols, entityTableCols, null);
    }

    public Map.Entry<Long, Long> insertNewEntity(Contract.EntityTable table,
                                                 JSONObject jsonDataRow,
                                                 long insertingId,
                                                 String[] from,
                                                 String[] to) throws JSONException {
        ContentValues data = getContentValuesFromJson(jsonDataRow, from, to);
        data.put(table.REMOTE_ID, insertingId);
        data.put(table.ENTITY_STATUS, Contract.STATUS_IDLE);
        Uri result = mResolver.insert(table.URI, data);
        long localInsertedId = Long.valueOf(result.getLastPathSegment());
        return new AbstractMap.SimpleEntry<Long, Long>(insertingId, localInsertedId);
    }

    public void updateSingleEntity(Contract.EntityTable localTable, long localId, JSONObject jsonData, String[] from, String[] to) throws JSONException {
        updateSingleEntity(localTable, localId, jsonData, from, to, null);
    }

    public void updateSingleEntity(Contract.EntityTable localTable, long localId, JSONObject jsonData,
                                   String[] from, String[] to, PreCallbacks callback) throws JSONException {
        ContentValues data = getContentValuesFromJson(jsonData, from, to);
        if((callback != null && callback.onPreUpdate(localTable, localId, data)) || callback == null) {
            mResolver.update(localTable.URI, data, String.format("%s = %d", localTable._ID, localId), null);
        }
    }

    private ContentValues getContentValuesFromJson(JSONObject json, String[] from, String[] to) throws JSONException {
        ContentValues data = new ContentValues();
        for (int i = 0; i < from.length; i++) {
            data.put(to[i], json.getString(from[i]));
        }
        return data;
    }

    public long insertTempEntity(Contract.EntityTable localTable, ContentValues data) {
        data.put(localTable.ENTITY_STATUS, Contract.STATUS_INSERTING);
        Uri result = mResolver.insert(localTable.URI, data);
        return Long.valueOf(result.getLastPathSegment());
    }

    public void persistTempEntity(Contract.EntityTable table,
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

    public void persistTempEntities(Contract.EntityTable table, long[] localIds, long[] remoteIds) {
        for (int i = 0; i < localIds.length; i++) {
            persistTempEntity(table, localIds[i], remoteIds[i]);
        }
    }

    public void persistTempEntities(Contract.EntityTable table, long[] localTempIds, JSONArray remoteIds) throws Exception {
        persistTempEntities(table, localTempIds, Utils.getIdsFromJSONArray(remoteIds));
    }

    public void persistUpdatingEntity(Contract.EntityTable table, long localId, ContentValues data) {
        data.put(table.ENTITY_STATUS, Contract.STATUS_IDLE);
        mResolver.update(table.URI, data, table._ID + " = ?", new String[]{String.valueOf(localId)});
    }

    public void deleteMarkedEntities(Contract.EntityTable table) {
        mResolver.delete(
                table.URI,
                table.ENTITY_STATUS + " = ?",
                new String[]{String.valueOf(Contract.STATUS_DELETING)}
        );
    }

    public void deleteEntityByLocalId(Contract.EntityTable table, long localId) {
        String[] args = new String[]{String.valueOf(localId)};
        mResolver.delete(table.URI, table._ID + " = ?", args);
    }

    public void deleteEntitiesByLocalIds(Contract.EntityTable table, long[] localIds) {
        for (long id : localIds) {
            deleteEntityByLocalId(table, id);
        }
    }

    public void deleteEntitiesByLocalIds(Contract.EntityTable table, JSONArray localIds) throws Exception {
        deleteEntitiesByLocalIds(table, Utils.getIdsFromJSONArray(localIds));
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

    /*public void markEntityAsIdle(Contract.EntityTable table, long localId) {
        markEntityWithStatus(table, localId, Contract.STATUS_IDLE);
    }

    public void markEntitiesAsIdle(Contract.EntityTable table, long[] localIds) {
        markEntitiesWithStatus(table, localIds, Contract.STATUS_IDLE);
    }

    public void markEntitiesAsIdle(Contract.EntityTable table, JSONArray localIds) throws Exception {
        markEntitiesAsIdle(table, Utils.getIdsFromJSONArray(localIds));
    }

    public void markEntityAsUpdating(Contract.EntityTable table, long localId) {
        markEntityWithStatus(table, localId, Contract.STATUS_UPDATING);
    }

    public void markEntitiesAsUpdating(Contract.EntityTable table, long[] localIds) {
        markEntitiesWithStatus(table, localIds, Contract.STATUS_UPDATING);
    }

    public void markEntitiesAsUpdating(Contract.EntityTable table, JSONArray localIds) throws Exception {
        markEntitiesAsUpdating(table, Utils.getIdsFromJSONArray(localIds));
    }

    public void markEntityAsDeleting(Contract.EntityTable table, long localId) {
        markEntityWithStatus(table, localId, Contract.STATUS_DELETING);
    }

    public void markEntitiesAsDeleting(Contract.EntityTable table, long[] localIds) {
        markEntitiesWithStatus(table, localIds, Contract.STATUS_DELETING);
    }

    public void markEntitiesAsDeleting(Contract.EntityTable table, JSONArray localIds) throws Exception {
        markEntitiesAsDeleting(table, Utils.getIdsFromJSONArray(localIds));
    }

    private void markEntityWithStatus(Contract.EntityTable table, long localId, int status) {
        ContentValues data = new ContentValues();
        data.put(table.ENTITY_STATUS, status);
        mResolver.update(table.URI, data, table._ID + " = ?", new String[]{String.valueOf(localId)});
    }

    private void markEntitiesWithStatus(Contract.EntityTable table, long[] localIds, int status) {
        for (long id : localIds) {
            markEntityWithStatus(table, id, status);
        }
    }*/

    public interface PreCallbacks {
        public boolean onPreInsert(EntityTable table, ContentValues toUpdate);
        public boolean onPreUpdate(EntityTable table, long localId, ContentValues toUpdate);
        public boolean onPreDelete(EntityTable table, long localId);
    }

    public abstract static class PreCallbacksAdapter implements PreCallbacks {
        @Override
        public boolean onPreInsert(EntityTable table, ContentValues toUpdate) {
            return true;
        }

        @Override
        public boolean onPreUpdate(EntityTable table, long localId, ContentValues toUpdate) {
            return true;
        }

        @Override
        public boolean onPreDelete(EntityTable table, long localId) {
            return true;
        }
    }

    public interface PostCallbacks {
        public void onInserted(long localId);
        public void onUpdated(long localId);
        public void onDeleted(long localId);
    }

    public static abstract class PostCallbacksAdapter implements PostCallbacks {}
}
