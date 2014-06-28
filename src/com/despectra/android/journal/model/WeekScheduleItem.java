package com.despectra.android.journal.model;

import com.despectra.android.journal.logic.local.Contract;

/**
 * Created by Dmitry on 20.06.14.
 */
public class WeekScheduleItem {
    public JoinedEntityIds scheduleIds;
    public String label1;
    public String label2;
    public int color;
    public int status;

    public WeekScheduleItem(JoinedEntityIds scheduleIds, String label1, String label2, int color) {
        this.scheduleIds = scheduleIds;
        this.label1 = label1;
        this.label2 = label2;
        this.color = color;
        this.status = Contract.STATUS_IDLE;
    }

    public WeekScheduleItem(JoinedEntityIds scheduleIds, String label1, String label2, int color, int status) {
        this.scheduleIds = scheduleIds;
        this.label1 = label1;
        this.label2 = label2;
        this.color = color;
        this.status = status;
    }

}
