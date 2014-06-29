package com.despectra.android.journal.model;

/**
 * Created by Dmitry on 29.06.14.
 */
public class Mark {
    public EntityIds ids;
    public int status;
    public String mark;

    public Mark(EntityIds ids, int status, String mark) {
        this.ids = ids;
        this.status = status;
        this.mark = mark;
    }
}
