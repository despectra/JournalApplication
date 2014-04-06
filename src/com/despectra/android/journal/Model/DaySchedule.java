package com.despectra.android.journal.Model;

import android.util.SparseArray;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Dmitry on 05.04.14.
 */
public class DaySchedule {
    private SparseArray<ScheduleItem> items;

    public DaySchedule() {
        items = new SparseArray<ScheduleItem>(10);
    }

    public void addItem(int lessonNum, ScheduleItem item) {
        items.put(lessonNum, item);
    }

    public ScheduleItem getItem(int lessonNum) {
        return items.get(lessonNum, null);
    }

    public static DaySchedule fromJson(JSONObject json) throws JSONException{
        DaySchedule daySchedule = new DaySchedule();
        JSONArray lessons = json.getJSONArray("lessons");
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = lessons.getJSONObject(i);
            daySchedule.addItem(lesson.getInt("lessonNum"), ScheduleItem.fromJson(lesson));
        }
        return daySchedule;
    }

    @Override
    public String toString() {
        String repres = String.format("Lessons count: %d\nLessons: ", items.size());
        for (int i = 0; i < items.size(); i++) {
            repres += String.format("\n    No.%d, %s",items.keyAt(i), items.valueAt(i).toString());
        }
        return repres;
    }
}
