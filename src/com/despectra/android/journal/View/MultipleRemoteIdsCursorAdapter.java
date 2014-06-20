package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;

/**
 * Created by Dmitry on 10.04.14.
 */
public class MultipleRemoteIdsCursorAdapter extends RemoteIdsCursorAdapter {

    private int mCheckBoxId;
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
        super(context, layout, c, idsColumns, entityStatusColumn, from, to, flags);
        mCheckBoxId = checkBoxId;
        mPopupMenuBtnId = popupMenuBtn;
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
                        view.setBackgroundResource(R.drawable.list_item_inserting_bg);
                        break;
                    case Contract.STATUS_UPDATING:
                        view.setBackgroundResource(R.drawable.list_item_updating_bg);
                        break;
                    case Contract.STATUS_DELETING:
                        view.setBackgroundResource(R.drawable.list_item_deleting_bg);
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
                        mItemClickListener.onItemClick(view, position, ids);
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
                    setItemChecking(position, ids, checked);
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

    public interface OnItemPopupMenuListener {
        public void onMenuItemSelected(MenuItem item, View adapterItemView, JoinedEntityIds ids);

    }
}