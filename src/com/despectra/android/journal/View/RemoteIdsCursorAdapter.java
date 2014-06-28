package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmitry on 18.06.14.
 */
public abstract class RemoteIdsCursorAdapter extends SimpleCursorAdapter {

    public static final int FLAG_SELECTABLE = 1;
    public static final int FLAG_CHECKABLE = 2;
    public static final int FLAG_SPINNER_ADAPTER = 4;

    protected Context mContext;
    protected EntityIdsColumns[] mIdsColumns;
    protected String mEntityStatusColName;
    protected int mEntityStatusColId;
    protected Map<Long, JoinedEntityIds> mCheckedItemIds;
    protected JoinedEntityIds mSelectedItemIds;
    protected long mSelectedItemId;
    protected OnItemClickListener mItemClickListener;
    protected OnItemCheckedListener mItemCheckedListener;

    protected int mInteractionFlags;

    public RemoteIdsCursorAdapter(Context context, int layout, Cursor c, EntityIdsColumns[] idsColumns, String entityStatusColumn,
                                  String[] from, int[] to) {
        super(context, layout, c, from, to, 0);
        init(context, idsColumns, entityStatusColumn, 0);
    }

    public RemoteIdsCursorAdapter(Context context, int layout, Cursor c, EntityIdsColumns[] idsColumns, String entityStatusColumn,
                                  String[] from, int[] to, int interactionFlags) {
        super(context, layout, c, from, to, 0);
        init(context, idsColumns, entityStatusColumn, interactionFlags);
    }

    private void init(Context context, EntityIdsColumns[] idsColumns, String entityStatusColumn, int interactionFlags) {
        mContext = context;
        mIdsColumns = idsColumns;
        mEntityStatusColName = entityStatusColumn;
        mCheckedItemIds = new HashMap<Long, JoinedEntityIds>();
        mInteractionFlags = interactionFlags;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void setOnItemCheckedListener(OnItemCheckedListener listener) {
        mItemCheckedListener = listener;
    }

    public Context getContext() {
        return mContext;
    }

    public boolean isSelectable() {
        return (mInteractionFlags & FLAG_SELECTABLE) == FLAG_SELECTABLE;
    }

    public boolean isCheckable() {
        return (mInteractionFlags & FLAG_CHECKABLE) == FLAG_CHECKABLE;
    }

    public boolean isSpinnerAdapter() {
        return (mInteractionFlags & FLAG_SPINNER_ADAPTER) == FLAG_SPINNER_ADAPTER;
    }

    public JoinedEntityIds getItemIds(int position) {
        Cursor dataCursor = getCursor();
        if (dataCursor != null) {
            dataCursor.moveToPosition(position);
            return JoinedEntityIds.fromCursor(dataCursor, mIdsColumns);
        }
        return null;
    }

    public void setCheckedNone() {
        if (isCheckable()) {
            mCheckedItemIds.clear();
            notifyDataSetChanged();
        }
    }

    public Bundle getCheckedItems() {
        Bundle bundle = new Bundle();
        for (Long key : mCheckedItemIds.keySet()) {
            bundle.putBundle(key.toString(), mCheckedItemIds.get(key).toBundle());
        }
        return bundle;
    }

    public JoinedEntityIds[] getCheckedIds() {
        if (isCheckable()) {
            JoinedEntityIds[] ids = new JoinedEntityIds[mCheckedItemIds.size()];
            int i = 0;
            for (Long key : mCheckedItemIds.keySet()) {
                ids[i++] = mCheckedItemIds.get(key);
            }
            return ids;
        }
        return null;
    }

    public EntityIds[] getCheckedIdsOfTable(String table) {
        if (isCheckable()) {
            EntityIds[] ids = new EntityIds[mCheckedItemIds.size()];
            int i = 0;
            for (Long key : mCheckedItemIds.keySet()) {
                ids[i++] = mCheckedItemIds.get(key).getIdsByTable(table);
            }
            return ids;
        }
        return null;
    }

    public JoinedEntityIds getSelectedEntityIds() {
        return mSelectedItemIds;
    }

    public boolean isItemSelected(long id) {
        return isSelectable() && id == mSelectedItemId;
    }

    public EntityIds getSelectedEntityIdsByTable(String tableName) {
        if (isSelectable()) {
            if (mSelectedItemIds != null) {
                return mSelectedItemIds.getIdsByTable(tableName);
            }
        }
        return null;
    }

    public void restoreCheckedItems(Bundle checkedItems, boolean notifyAdapter) {
        if (isCheckable()) {
            for (String key : checkedItems.keySet()) {
                Long newKey = Long.valueOf(key);
                JoinedEntityIds newValue = JoinedEntityIds.fromBundle(checkedItems.getBundle(key));
                mCheckedItemIds.put(newKey, newValue);
            }
            if (notifyAdapter) {
                notifyDataSetChanged();
            }
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

    protected void setItemChecking(int position, JoinedEntityIds ids, boolean checked) {
        if (isCheckable()) {
            if (checked) {
                mCheckedItemIds.put(getItemId(position), ids);
            } else {
                mCheckedItemIds.remove(getItemId(position));
            }
            if (mItemCheckedListener != null) {
                mItemCheckedListener.onItemChecked(ids, mCheckedItemIds.size(), checked);
            }
        }
    }

    protected void setItemSelected(int position, JoinedEntityIds ids) {
        if (isSelectable()) {
            long itemId = getItemId(position);
            if (mSelectedItemId != itemId) {
                mSelectedItemIds = ids;
                mSelectedItemId = itemId;
            }
        }
    }

    public void setItemSelectedAtPos(int position, Cursor dataCursor) {
        if (isSelectable()) {
            dataCursor.moveToPosition(position);
            JoinedEntityIds ids = JoinedEntityIds.fromCursor(dataCursor, mIdsColumns);
            setItemSelected(position, ids);
            notifyDataSetChanged();
        }
    }

    public interface OnItemCheckedListener {
        public void onItemChecked(JoinedEntityIds ids, int checkedCount, boolean checked);
    }

    public interface OnItemClickListener {
        public void onItemClick(View itemView, int position, JoinedEntityIds ids);
    }
}
