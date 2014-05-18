package com.despectra.android.journal.view.schedule;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import com.despectra.android.journal.Model.DaySchedule;

/**
 * Created by Андрей on 06.04.14.
 */
public class DayScheduleAdapter extends CursorAdapter {

    private DaySchedule mSchedule;

    public DayScheduleAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public DayScheduleAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        
        return super.swapCursor(newCursor);
    }
}
