package com.despectra.android.journal.view;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;

/**
 * Created by Dmitry on 19.06.14.
 */
public class SimpleRemoteIdsAdapter extends RemoteIdsCursorAdapter {

    private OnItemClickedListener mListener;

    public SimpleRemoteIdsAdapter(Context context,
                                  int layout,
                                  Cursor c,
                                  EntityIdsColumns[]idsColumns,
                                  String entityStatusColumn,
                                  String[] from,
                                  int[] to) {
        super(context, layout, c, idsColumns, entityStatusColumn, from, to, 0);
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mListener = listener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        Cursor cursor = getCursor();
        if (view != null && cursor != null) {
            final JoinedEntityIds ids = JoinedEntityIds.fromCursor(getCursor(), mIdsColumns);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onItemClickedListener(view, position, ids);
                    }
                }
            });
        }
        return view;
    }

    public interface OnItemClickedListener {
        public void onItemClickedListener(View clickedView, int position, JoinedEntityIds ids);
    }
}
