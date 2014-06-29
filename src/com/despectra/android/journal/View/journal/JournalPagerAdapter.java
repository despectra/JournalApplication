package com.despectra.android.journal.view.journal;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.TimedWeekScheduleItem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Dmirty on 17.02.14.
 */
public class JournalPagerAdapter extends FragmentStatePagerAdapter {

    private static final String TAG = "JOURNAL_PAGER_ADAPTER";
    private static final Map<Integer, Integer> DAYS;
    static {
        DAYS = new HashMap<Integer, Integer>();
        DAYS.put(Calendar.MONDAY, 1);
        DAYS.put(Calendar.TUESDAY, 2);
        DAYS.put(Calendar.WEDNESDAY, 3);
        DAYS.put(Calendar.THURSDAY, 4);
        DAYS.put(Calendar.FRIDAY, 5);
        DAYS.put(Calendar.SATURDAY, 6);
        DAYS.put(Calendar.SUNDAY, 7);
    }

    private Calendar mDaysOffset;
    private int mScheduleItemsPos;
    private DateFormat mDateFormat;
    List<TimedWeekScheduleItem> mScheduleItems;
    private List<EntityIds> mStudentsIds;

    public JournalPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public JournalPagerAdapter(JournalFragment.JournalAreaFragment hostFragment, FragmentManager fm, List<EntityIds> studentsIds,
                               List<TimedWeekScheduleItem> scheduleItems) {
        super(fm);
        mScheduleItems = scheduleItems;
        mDaysOffset = Calendar.getInstance();
        mDateFormat = new SimpleDateFormat("dd.MM.yy");
        mStudentsIds = studentsIds;
        determineInitDay();
    }

    private void determineInitDay() {
        int currentDay = DAYS.get(mDaysOffset.get(Calendar.DAY_OF_WEEK));
        for (int i = 0; i < mScheduleItems.size(); i++) {
            mScheduleItemsPos = i;
            if (mScheduleItems.get(i).day >= currentDay) {
                break;
            }
            mDaysOffset.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    @Override
    public Fragment getItem(int position) {
        Log.e("JOURNAL_ADAPTER", String.valueOf(position));
        JournalPageFragment fragment = new JournalPageFragment();
        Bundle[] days = new Bundle[6];
        for (int i = 0; i < 6; i++) {
            while(true) {
                int dayOnOffset = DAYS.get(mDaysOffset.get(Calendar.DAY_OF_WEEK));
                int closestLessonDay = mScheduleItems.get(mScheduleItemsPos).day;
                if (dayOnOffset == closestLessonDay) {
                    TimedWeekScheduleItem currentBusyDay = mScheduleItems.get(mScheduleItemsPos);
                    days[i] = new Bundle();
                    days[i].putString("dayRepresent", mDateFormat.format(mDaysOffset.getTime()));
                    days[i].putBundle("scheduleItem", currentBusyDay.toBundle());
                    if (mScheduleItemsPos == mScheduleItems.size() - 1) {
                        mScheduleItemsPos = 0;
                        mDaysOffset.add(Calendar.DAY_OF_YEAR, -1);
                    } else {
                        mScheduleItemsPos++;
                        if (closestLessonDay != mScheduleItems.get(mScheduleItemsPos).day) {
                            mDaysOffset.add(Calendar.DAY_OF_YEAR, -1);
                        }
                    }
                    break;
                }
                mDaysOffset.add(Calendar.DAY_OF_YEAR, -1);
            }
        }
        Bundle args = new Bundle();
        Bundle[] studentsIds = new Bundle[mStudentsIds.size()];
        for (int i = 0; i < mStudentsIds.size(); i++) {
            studentsIds[i] = mStudentsIds.get(i).toBundle();
        }
        args.putInt(JournalPageFragment.ARG_INDEX, position);
        args.putParcelableArray("days", days);
        args.putParcelableArray("students", studentsIds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        return 999;
    }

}
