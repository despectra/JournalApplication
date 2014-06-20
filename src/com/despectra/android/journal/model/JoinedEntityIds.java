package com.despectra.android.journal.model;

import android.database.Cursor;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Dmitry on 01.06.14.
 */
public class JoinedEntityIds {
    private Map<String, EntityIds> mIds;

    public JoinedEntityIds() {
        mIds = new HashMap<String, EntityIds>();
    }

    public JoinedEntityIds(String[] tables, long[] localIds, long[] remoteIds) {
        mIds = new HashMap<String, EntityIds>();
        if (tables != null) {
            for (int i = 0; i < tables.length; i++) {
                mIds.put(tables[i], new EntityIds(localIds[i], remoteIds[i]));
            }
        }
    }

    public JoinedEntityIds(String tables[], EntityIds[] ids) {
        mIds = new HashMap<String, EntityIds>();
        if (tables != null) {
            for (int i = 0; i < tables.length; i++) {
                mIds.put(tables[i], ids[i]);
            }
        }
    }

    public static JoinedEntityIds fromBundle(Bundle bundle) {
        String[] tables = bundle.keySet().toArray(new String[]{});
        EntityIds[] ids = new EntityIds[bundle.size()];
        for (int i = 0; i < tables.length; i++) {
            ids[i] = EntityIds.fromBundle(bundle.getBundle(tables[i]));
        }
        return new JoinedEntityIds(tables, ids);
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        for (String key : mIds.keySet()) {
            b.putBundle(key, mIds.get(key).toBundle());
        }
        return b;
    }

    public EntityIds getIdsByTable(String table) {
        return mIds.get(table);
    }

    public static JoinedEntityIds fromCursor(Cursor cursor, EntityIdsColumns[] idsColumns) {
        long[] localIds = new long[idsColumns.length],
                remoteIds = new long[idsColumns.length];
        String[] tables = new String[idsColumns.length];
        for (int i = 0; i < idsColumns.length; i++) {
            EntityIdsColumns columns = idsColumns[i];
            localIds[i] = cursor.getLong(columns.getLocalIdColumnIndex());
            remoteIds[i] = cursor.getLong(columns.getRemoteIdColumnIndex());
            tables[i] = columns.getTableName();
        }
        return new JoinedEntityIds(tables, localIds, remoteIds);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JoinedEntityIds)) {
            return false;
        }
        JoinedEntityIds that = (JoinedEntityIds) o;
        return mIds.equals(that);
    }
}
