package com.despectra.android.journal.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.despectra.android.journal.R;

/**
 * Created by Dmitry on 03.06.14.
 */
public abstract class PagerContainerFragment extends Fragment {

    protected ViewPager mPager;
    protected PagerTabStrip mTabStrip;
    protected PagerAdapter mPagerAdapter;

    public PagerContainerFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pager_container, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mPager = (ViewPager) getView().findViewById(R.id.fragment_main_page_single);
        mPagerAdapter = new PagerAdapter(getChildFragmentManager()) {

            @Override
            public int getItemPosition(Object object) {
                return POSITION_NONE;
            }
        };
        /*mPager.post(new Runnable() {
            @Override
            public void run() {
                mPager.setAdapter(mPagerAdapter);
            }
        });*/
        mPager.setAdapter(mPagerAdapter);
        mTabStrip = (PagerTabStrip) getView().findViewById(R.id.pager_tab_strip);
        mTabStrip.setTabIndicatorColorResource(android.R.color.holo_blue_dark);
        super.onActivityCreated(savedInstanceState);
    }

    public abstract Fragment getPagerItem(int position);
    public abstract int getPagerItemsCount();
    public abstract String getPagerItemTitle(int position);

    public float getPagerItemWidth(int position) {
        return 1;
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return getPagerItem(i);
        }

        @Override
        public int getCount() {
            return getPagerItemsCount();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getPagerItemTitle(position);
        }
        @Override
        public float getPageWidth(int position) {
            return getPagerItemWidth(position);
        }

    }
}
