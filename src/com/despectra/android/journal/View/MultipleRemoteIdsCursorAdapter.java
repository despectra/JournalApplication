package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;

import java.util.*;

/**
 * Created by Dmitry on 10.04.14.
 */
public class MultipleRemoteIdsCursorAdapter extends SimpleCursorAdapter {

    private int mEntityStatusColId;
    private EntityIdsColumns[] mIdsColumns;
    private String mEntityStatusColName;
    private Context mContext;
    private OnItemClickListener mItemClickListener;
    private int mCheckBoxId;
    private OnItemCheckedListener mItemCheckedListener;
    private Map<Long, JoinedEntityIds> mCheckedItemIds;
    private int mPopupMenuBtnId;
    private int mPopupMenuRes;
    private OnItemPopupMenuListener mPopupMenuListener;

    public MultipleRemoteIdsCursorAdapter(Context context,
                                          int layout,
                                          Cursor c,
                                          String[] from,
                                          int[] to,
                                          EntityIdsColumns[] idsColumns,
                                          String entityStatusColumn,
                                          int checkBoxId,
                                          int popupMenuBtn,
                                          int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
        mIdsColumns = idsColumns;
        mEntityStatusColName = entityStatusColumn;
        mCheckBoxId = checkBoxId;
        mPopupMenuBtnId = popupMenuBtn;
        mCheckedItemIds = new HashMap<Long, JoinedEntityIds>();
    }

    public void setOnItemCheckedListener(OnItemCheckedListener listener) {
        mItemCheckedListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void setItemPopupMenu(int menuRes, OnItemPopupMenuListener menuListener) {
        mPopupMenuRes = menuRes;
        mPopupMenuListener = menuListener;
        notifyDataSetChanged();
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
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        Cursor cursor = getCursor();
        if (cursor != null) {
            cursor.moveToPosition(position);
            final long localId = getItemId(position);
            final JoinedEntityIds ids = JoinedEntityIds.fromCursor(cursor, mIdsColumns);

            boolean isItemChecked = mCheckedItemIds.containsKey(localId);
            int status = cursor.getInt(mEntityStatusColId);

            final CheckBox checkBox = (CheckBox) view.findViewById(mCheckBoxId);
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(isItemChecked);
            final ImageButton popupBtn = (ImageButton) view.findViewById(mPopupMenuBtnId);
            if (status != Contract.STATUS_IDLE) {
                view.setEnabled(false);
                checkBox.setEnabled(false);
                popupBtn.setEnabled(false);
                switch (status) {
                    case Contract.STATUS_INSERTING:
                        view.setBackgroundColor(mContext.getResources().getColor(R.color.item_inserting));
                        break;
                    case Contract.STATUS_UPDATING:
                        view.setBackgroundColor(mContext.getResources().getColor(R.color.item_updating));
                        break;
                    case Contract.STATUS_DELETING:
                        view.setBackgroundColor(mContext.getResources().getColor(R.color.item_deleting));
                        break;
                }
            } else {
                view.setEnabled(true);
                checkBox.setEnabled(true);
                popupBtn.setEnabled(true);
                view.setBackgroundResource(R.drawable.item_checkable_background);
            }
            popupBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View anchorView) {
                    showItemPopupMenu(anchorView, view, ids);
                }
            });
            view.setActivated(isItemChecked);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClick(view, ids);
                    }
                }
            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    checkBox.setChecked(true);
                    return true;
                }
            });
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    view.setActivated(checked);
                    if (checked) {
                        mCheckedItemIds.put(localId, ids);
                    } else {
                        mCheckedItemIds.remove(localId);
                    }
                    if (mItemCheckedListener != null) {
                        mItemCheckedListener.onItemChecked(ids, mCheckedItemIds.size(), checked);
                    }
                }
            });
        }
        return view;
    }

    private void showItemPopupMenu(View anchorView, final View adapterItemView, final JoinedEntityIds ids) {
        if (mPopupMenuRes == 0) {
            return;
        }
        PopupMenu menu = new PopupMenu(mContext, anchorView);
        menu.inflate(mPopupMenuRes);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (mPopupMenuListener != null) {
                    mPopupMenuListener.onMenuItemSelected(menuItem, adapterItemView, ids);
                }
                return true;
            }
        });
        menu.show();
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

    public interface OnItemCheckedListener {
        public void onItemChecked(JoinedEntityIds ids, int checkedCount, boolean checked);
    }

    public interface OnItemClickListener {
        public void onItemClick(View itemView, JoinedEntityIds ids);
    }

    public interface OnItemPopupMenuListener {
        public void onMenuItemSelected(MenuItem item, View adapterItemView, JoinedEntityIds ids);

    }
}