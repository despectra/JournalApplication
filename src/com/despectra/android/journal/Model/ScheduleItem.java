package com.despectra.android.journal.Model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Dmitry on 05.04.14.
 */
public class ScheduleItem {
    public long id;
    public int subjectId;
    public int groupId;
    public int teacherId;
    public String subject;
    public String group;
    public String teacher;

    public ScheduleItem(long id, int subjectId, int groupId, int teacherId, String subject, String group, String teacher) {
        this.id = id;
        this.subjectId = subjectId;
        this.groupId = groupId;
        this.teacherId = teacherId;
        this.subject = subject;
        this.group = group;
        this.teacher = teacher;
    }

    public static ScheduleItem fromJson(JSONObject json) throws JSONException {
        return new ScheduleItem(
                json.getInt("id"),
                json.getInt("subjectId"),
                json.getInt("groupId"),
                json.getInt("teacherId"),
                json.getString("subject"),
                json.getString("group"),
                json.getString("teacher")
        );
    }

    @Override
    public String toString() {
        return String.format("\n........id: %d, subject: %s, group %s, teacher %s", id, subject, group, teacher);
    }
}
