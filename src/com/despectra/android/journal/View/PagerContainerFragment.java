package com.despectra.android.journal.view;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

/**
 * Created by Dmitry on 03.06.14.
 */
public class PagerContainerFragment extends Fragment {

    private ViewPager mPager;
    private PagerTabStrip mTabStrip;
    private PagerAdapter mPagerAdapter;

    private static class PagerAdapter extends FragmentStatePagerAdapter {
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return null;
        }

        @Override
        public int getCount() {
            return 0;
        }
    }
}
