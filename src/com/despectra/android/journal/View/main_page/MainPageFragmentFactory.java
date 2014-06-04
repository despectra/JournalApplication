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
    public static final String FRAGMENT_TAG = "MainPageFragment";

    public static Fragment instantiate(Context context, FragmentManager fm) {
        Fragment fragment = fm.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            if (Utils.getScreenCategory(context) < Configuration.SCREENLAYOUT_SIZE_LARGE) {
                //for handsets
                fragment = new MainPageSmallFragment();
            } else {
                //for tablets
                fragment = new MainPageLargeFragment();
            }
        }
        return fragment;
    }
}
