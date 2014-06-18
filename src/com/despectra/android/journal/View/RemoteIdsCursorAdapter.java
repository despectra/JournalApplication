package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmitry on 18.06.14.
 */
public abstract class RemoteIdsCursorAdapter extends SimpleCursorAdapter {
    protected Context mContext;
    protected EntityIdsColumns[] mIdsColumns;
    protected String mEntityStatusColName;
    protected int mEntityStatusColId;
    protected Map<Long, JoinedEntityIds> mCheckedItemIds;

    public RemoteIdsCursorAdapter(Context context, int layout, Cursor c, EntityIdsColumns[] idsColumns, String entityStatusColumn,
                                  String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
        mIdsColumns = idsColumns;
        mEntityStatusColName = entityStatusColumn;
        mCheckedItemIds = new HashMap<Long, JoinedEntityIds>();
    }

    public Context getContext() {
        return mContext;
    }

    public void setCheckedNone() {
        mCheckedItemIds.clear();
        notifyDataSetChanged();
    }

    public Bundle getCheckedItems() {
        Bundle bundle = new Bundle();
        for (Long key : mCheckedItemIds.keySet()) {
            bundle.putBundle(key.toString(), mCheckedItemIds.get(key).toBundle());
        }
        return bundle;
    }

    public JoinedEntityIds[] getCheckedIds() {
        JoinedEntityIds[] ids = new JoinedEntityIds[mCheckedItemIds.size()];
        int i = 0;
        for (Long key : mCheckedItemIds.keySet()) {
            ids[i++] = mCheckedItemIds.get(key);
        }
        return ids;
    }

    public EntityIds[] getCheckedIdsOfTable(String table) {
        EntityIds[] ids = new EntityIds[mCheckedItemIds.size()];
        int i = 0;
        for (Long key : mCheckedItemIds.keySet()) {
            ids[i++] = mCheckedItemIds.get(key).getIdsByTable(table);
        }
        return ids;
    }

    public void restoreCheckedItems(Bundle checkedItems, boolean notifyAdapter) {
        for (String key : checkedItems.keySet()) {
            Long newKey = Long.valueOf(key);
            JoinedEntityIds newValue = JoinedEntityIds.fromBundle(checkedItems.getBundle(key));
            mCheckedItemIds.put(newKey, newValue);
        }
        if (notifyAdapter) {
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        if (c != null) {
            for (EntityIdsColumns column : mIdsColumns) {
                column.updateColumnsIndexes(c);
            }
            mEntityStatusColId = c.getColumnIndexOrThrow(mEntityStatusColName);
        }
        return super.swapCursor(c);
    }
}
