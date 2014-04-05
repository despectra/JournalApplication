package com.despectra.android.journal.Model;

import android.util.SparseArray;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.IllegalFormatException;

/**
 * Created by Dmitry on 05.04.14.
 */
public class WeekSchedule {
    public static final int MON = 0;
    public static final int TUE = 1;
    public static final int WED = 2;
    public static final int THU = 3;
    public static final int FRI = 4;
    public static final int SAT = 5;
    public static final int SUN = 6;

    private SparseArray<DaySchedule> dayItems;

    public WeekSchedule() {
        dayItems = new SparseArray<DaySchedule>();
    }


    public void addDayItem(int dayOfWeek, DaySchedule item) {
        dayItems.put(dayOfWeek, item);
    }

    public DaySchedule getDayItem(int dayOfWeek) {
        return dayItems.get(dayOfWeek);
    }

    public void addScheduleItem(int dayOfWeek, int lessonNum, ScheduleItem item) {
        DaySchedule schedByDay = dayItems.get(dayOfWeek, new DaySchedule());
        schedByDay.addItem(lessonNum, item);
    }

    public ScheduleItem getScheduleItem(int dayOfWeek, int lessonNum) {
        DaySchedule schedByDay = dayItems.get(dayOfWeek, null);
        if (schedByDay != null) {
            return schedByDay.getItem(lessonNum);
        } else {
            return null;
        }
    }

    public static WeekSchedule fromJson(JSONObject jsonObject) throws JSONException {
        WeekSchedule schedule = new WeekSchedule();
        JSONArray days = jsonObject.getJSONArray("days");
        for (int i = 0; i < days.length(); i++) {
            JSONObject day = days.getJSONObject(i);
            int dayOfWeek = day.getInt("dayOfWeek");
            if(dayOfWeek < 0 || dayOfWeek > 6) {
                throw new IllegalArgumentException("Wrong JSON data describing day of week");
            }
            schedule.addDayItem(dayOfWeek, DaySchedule.fromJson(day.getJSONArray("lessons")));
        }
        return schedule;
    }
}
