package com.despectra.android.journal.view.main_page;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.PagerContainerFragment;

/**
 * Created by Dmitry on 03.06.14.
 */
public class MainPageFragmentFactory {
    public static Fragment instantiate(Context context, FragmentManager fm) {
        if (Utils.getScreenCategory(context) < Configuration.SCREENLAYOUT_SIZE_LARGE) {
            //for handsets
            return new PagerContainerFragment() {
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
            };
        } else {
            //for tablets
            return new MainPageFragment();
        }
    }
}
