package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;

/**
 * Created by Dmitry on 19.06.14.
 */
public class SimpleRemoteIdsAdapter extends RemoteIdsCursorAdapter {

    private static final String TAG = "SimpleRemoteIdsAdapter";

    public SimpleRemoteIdsAdapter(Context context,
                                  int layout,
                                  Cursor c,
                                  EntityIdsColumns[]idsColumns,
                                  String entityStatusColumn,
                                  String[] from,
                                  int[] to,
                                  int interactionFlags) {
        super(context, layout, c, idsColumns, entityStatusColumn, from, to, interactionFlags);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        Cursor cursor = getCursor();
        if (view != null && cursor != null) {
            long itemId = getItemId(position);
            final JoinedEntityIds ids = JoinedEntityIds.fromCursor(getCursor(), mIdsColumns);
            if (!isSpinnerAdapter()) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setItemSelected(position, ids);
                        if (mItemClickListener != null) {
                            mItemClickListener.onItemClick(view, position, ids);
                        }
                        notifyDataSetChanged();
                    }
                });
                view.setBackgroundResource(isItemSelected(itemId) ? R.drawable.selected_list_item_bg : R.drawable.item_checkable_background);
                TextView text1;
                if ((text1 = (TextView)view.findViewById(R.id.text1)) != null) {
                    text1.setTextColor(isItemSelected(itemId) ? Color.parseColor("#efefef") : Color.parseColor("#505050"));
                }
            }
            Log.e(TAG, position + " " + isItemSelected(itemId) + " " + view.isSelected());
        }
        return view;
    }
}
