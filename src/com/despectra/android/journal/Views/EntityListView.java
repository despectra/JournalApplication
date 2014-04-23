package com.despectra.android.journal.Views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;
import com.despectra.android.journal.Adapters.RemoteIdCursorAdapter;

/**
 * Created by Dmitry on 15.04.14.
 */
public class EntityListView extends ListView {
    private RemoteIdCursorAdapter mAdapter;

    public EntityListView(Context context) {
        super(context);
    }

    public EntityListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EntityListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


}
