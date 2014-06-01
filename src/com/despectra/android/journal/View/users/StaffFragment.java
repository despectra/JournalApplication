package com.despectra.android.journal.view.users;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.FrameLayout;
import com.despectra.android.journal.R;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.AbstractApiFragment;
import com.despectra.android.journal.view.EntitiesListFragment;
import com.despectra.android.journal.view.RemoteIdCursorAdapter;
import com.despectra.android.journal.view.SimpleConfirmationDialog;
import com.despectra.android.journal.view.main_page.CurrentDayScheduleFragment;
import com.despectra.android.journal.view.main_page.WallFragment;

/**
 * Created by Dmitry on 07.04.14.
 */
public class StaffFragment extends Fragment {

    public static final String FRAGMENT_TAG = "StaffFragment";

    private ViewPager mPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_staff, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mPager = (ViewPager) getView().findViewById(R.id.fragment_main_page_single);
        final PagerAdapter pagerAdapter = new PagerAdapter(getChildFragmentManager());
        mPager.post(new Runnable() {
            @Override
            public void run() {
                mPager.setAdapter(pagerAdapter);
            }
        });
        PagerTabStrip tabStrip = (PagerTabStrip) getView().findViewById(R.id.pager_tab_strip);
        tabStrip.setTabIndicatorColorResource(android.R.color.holo_blue_dark);
        super.onActivityCreated(savedInstanceState);
    }

    public static class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
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
                    return "Учителя";
                case 1:
                    return "Завучи и администраторы";
                default:
                    return "";
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        private Fragment initFirstFragmet() {
            return new TeachersFragment();
        }

        private Fragment initSecondFragment() {
            return new AdminsFragment();
        }
    }
}
