package com.despectra.android.journal.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.despectra.android.journal.Data.Contract;
import com.despectra.android.journal.R;

import java.util.*;

/**
 * Created by Dmitry on 10.04.14.
 */
public class RemoteIdCursorAdapter extends SimpleCursorAdapter {

    private int mCheckBoxId;
    private int mPopupMenuBtnId;
    private int mIdColIndex;
    private int mRemoteIdColIndex;
    private int mEntityStatusColId;
    private Context mContext;
    private OnItemCheckedListener mListener;
    private Map<Long, Long> mCheckedItemIds;
    private int mPopupMenuRes;
    private OnItemPopupMenuListener mPopupMenuListener;

    public RemoteIdCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int checkBoxId, int popupMenuBtn, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
        mCheckBoxId = checkBoxId;
        mPopupMenuBtnId = popupMenuBtn;
        mCheckedItemIds = new HashMap<Long, Long>();
    }

    public void setOnItemCheckedListener(OnItemCheckedListener listener) {
        mListener = listener;
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

    public long[] getCheckedLocalIdsAsArray() {
        if (mCheckedItemIds != null && mCheckedItemIds.size() > 0) {
            long[] items = new long[mCheckedItemIds.size()];
            int i = 0;
            for (Long key : mCheckedItemIds.keySet()) {
                items[i] = key;
                i++;
            }
            return items;
        }
        return new long[]{};
    }

    public long[] getCheckedRemoteIdsAsArray() {
        if (mCheckedItemIds != null && mCheckedItemIds.size() > 0) {
            long[] items = new long[mCheckedItemIds.size()];
            int i = 0;
            for (Long value : mCheckedItemIds.values()) {
                items[i] = value;
                i++;
            }
            return items;
        }
        return new long[]{};
    }

    public void setCheckedItemIdsAsArray(long[] localIds, long[] remoteIds, boolean notifyAdapter) {
        for (int i = 0; i < localIds.length; i++) {
            mCheckedItemIds.put(localIds[i], remoteIds[i]);
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
            final long remoteId = cursor.getLong(mRemoteIdColIndex);
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
                    showItemPopupMenu(anchorView, view, localId, remoteId);
                }
            });
            view.setActivated(isItemChecked);
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
                        mCheckedItemIds.put(localId, remoteId);
                    } else {
                        mCheckedItemIds.remove(localId);
                    }
                    if (mListener != null) {
                        mListener.onItemChecked(localId, remoteId, mCheckedItemIds.size(), checked);
                    }
                }
            });
        }
        return view;
    }

    private void showItemPopupMenu(View anchorView, final View adapterItemView, final long localId, final long remoteId) {
        PopupMenu menu = new PopupMenu(mContext, anchorView);
        menu.inflate(mPopupMenuRes);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (mPopupMenuListener != null) {
                    mPopupMenuListener.onMenuItemSelected(menuItem, adapterItemView, localId, remoteId);
                }
                return true;
            }
        });
        menu.show();
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        if (c != null) {
            mIdColIndex = c.getColumnIndexOrThrow(Contract.FIELD_ID);
            mRemoteIdColIndex = c.getColumnIndexOrThrow(Contract.FIELD_REMOTE_ID);
            mEntityStatusColId = c.getColumnIndexOrThrow(Contract.FIELD_ENTITY_STATUS);
        }
        return super.swapCursor(c);
    }

    public interface OnItemCheckedListener {
        public void onItemChecked(long localId, long remoteId, int checkedCount, boolean checked);
    }

    public interface OnItemPopupMenuListener {
        public void onMenuItemSelected(MenuItem item, View adapterItemView, long listItemLocalId, long listItemRemoteId);
    }
}
