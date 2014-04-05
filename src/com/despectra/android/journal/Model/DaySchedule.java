package com.despectra.android.journal.Model;

import android.util.SparseArray;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

    public static DaySchedule fromJson(JSONArray jsonArray) throws JSONException{
        DaySchedule daySchedule = new DaySchedule();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject lesson = jsonArray.getJSONObject(i);
            daySchedule.addItem(lesson.getInt("lessonNum"), ScheduleItem.fromJson(lesson));
        }
        return daySchedule;
    }
}
