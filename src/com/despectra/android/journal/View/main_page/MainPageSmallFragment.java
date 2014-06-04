package com.despectra.android.journal.view.main_page;

import android.support.v4.app.Fragment;
import com.despectra.android.journal.view.PagerContainerFragment;

/**
 * Created by Dmitry on 04.06.14.
 */
public class MainPageSmallFragment extends PagerContainerFragment {
    public static final String FRAGMENT_TAG = "MainPageFragment";

    @Override
    public Fragment getPagerItem(int position) {
        switch (position) {
            case 0:
                return new WallFragment();
            case 1:
                return new CurrentDayScheduleFragment();
            default:
                return new Fragment();
        }

    }

    @Override
    public int getPagerItemsCount() {
        return 2;
    }

    @Override
    public String getPagerItemTitle(int position) {
        switch (position) {
            case 0:
                return "Стена";
            case 1:
                return "Расписание";
            default:
                return "";
        }
    }
}
