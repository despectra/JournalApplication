package com.despectra.android.journal.view.main_page;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.R;

/**
 * Created by Dmirty on 17.02.14.
 */
public class MainPageFragment extends Fragment {

    private ViewPager mPager;
    private FrameLayout mLargeFirstLayout;
    private FrameLayout mLargeSecondLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_page, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        if (Utils.getScreenCategory(getActivity()) < Configuration.SCREENLAYOUT_SIZE_LARGE) {
            //for handsets
            mPager = (ViewPager) getView().findViewById(R.id.fragment_main_page_single);
            PagerAdapter pagerAdapter = new PagerAdapter(getChildFragmentManager());
            mPager.setAdapter(pagerAdapter);
            PagerTabStrip tabStrip = (PagerTabStrip) getView().findViewById(R.id.pager_tab_strip);
            tabStrip.setTabIndicatorColorResource(android.R.color.holo_blue_dark);
        } else {
            //for tablets
            mLargeFirstLayout = (FrameLayout) getView().findViewById(R.id.fragment_main_page_fst);
            mLargeSecondLayout = (FrameLayout) getView().findViewById(R.id.fragment_main_page_snd);
            WallFragment fstFragment = new WallFragment();
            CurrentDayScheduleFragment sndFragment = new CurrentDayScheduleFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.fragment_main_page_fst, fstFragment)
                    .replace(R.id.fragment_main_page_snd, sndFragment)
                    .commit();
        }
    }



    public static class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return initFirstFragmet();
                case 1:
                    return initSecondFragment();
                default:
                    return new Fragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0:
                    return "Стена";
                case 1:
                    return "Расписание";
                default:
                    return "";
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        private Fragment initFirstFragmet() {
            return new WallFragment();
        }

        private Fragment initSecondFragment() {
            return new CurrentDayScheduleFragment();
        }
    }
}
