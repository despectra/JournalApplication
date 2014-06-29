package com.despectra.android.journal.model;

import android.os.Bundle;

/**
 * Created by Dmitry on 30.06.14.
 */
public class TimedWeekScheduleItem {
    public int day;
    public int lesson;
    public EntityIds scheduleIds;

    public TimedWeekScheduleItem(int day, int lesson, EntityIds scheduleIds) {
        this.day = day;
        this.lesson = lesson;
        this.scheduleIds = scheduleIds;
    }

    public static TimedWeekScheduleItem fromBundle(Bundle bundle) {
        return new TimedWeekScheduleItem(
            bundle.getInt("day"),
            bundle.getInt("lesson"),
            EntityIds.fromBundle(bundle.getBundle("schedIds"))
        );
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putInt("day", day);
        b.putInt("lesson", lesson);
        b.putBundle("schedIds", scheduleIds.toBundle());
        return b;
    }
}
