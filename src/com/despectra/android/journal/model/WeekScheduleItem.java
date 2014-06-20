package com.despectra.android.journal.model;

/**
 * Created by Dmitry on 20.06.14.
 */
public class WeekScheduleItem {
    public EntityIds scheduleItemIds;
    public String label1;
    public String label2;
    public int color;

    public WeekScheduleItem(long localSchedItemId, long remoteSchedItemId, String label1, String label2, int color) {
        this.scheduleItemIds = new EntityIds(localSchedItemId, remoteSchedItemId);
        this.label1 = label1;
        this.label2 = label2;
        this.color = color;
    }
}
