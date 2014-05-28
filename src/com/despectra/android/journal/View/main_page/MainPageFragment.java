package com.despectra.android.journal.view.main_page;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
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

    private View mView;

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
        if (Utils.getScreenCategory(getActivity()) < Configuration.SCREENLAYOUT_SIZE_LARGE) {
            //for handsets
            final ActionBar bar = ((ActionBarActivity)getActivity()).getSupportActionBar();
            bar.removeAllTabs();
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            ActionBar.TabListener tabListener = new ActionBar.TabListener() {
                @Override
                public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                    mPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                }

                @Override
                public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                }
            };

            ActionBar.Tab fstTab = bar.newTab()
                    .setText("Стена")
                    .setTabListener(tabListener);
            ActionBar.Tab sndTab = bar.newTab()
                    .setText("Расписание")
                    .setTabListener(tabListener);

            mPager = (ViewPager) getView().findViewById(R.id.fragment_main_page_single);
            mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    bar.setSelectedNavigationItem(position);
                }
            });
            PagerAdapter pagerAdapter = new PagerAdapter(getChildFragmentManager());
            mPager.setAdapter(pagerAdapter);

            bar.addTab(fstTab);
            bar.addTab(sndTab);
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