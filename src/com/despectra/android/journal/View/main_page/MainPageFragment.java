package com.despectra.android.journal.view.main_page;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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

    private FrameLayout mSmallLayout;
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
            mSmallLayout = (FrameLayout) getView().findViewById(R.id.fragment_main_page_single);
            getChildFragmentManager().beginTransaction()
                    .add(R.id.fragment_main_page_single, new WallFragment())
                    .commit();

            ActionBar bar = ((ActionBarActivity)getActivity()).getSupportActionBar();
            bar.removeAllTabs();
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            ActionBar.Tab fstTab = bar.newTab()
                    .setText("Стена")
                    .setTabListener(
                            new TabListener<WallFragment>(getActivity(), "wallFragment", WallFragment.class));
            ActionBar.Tab sndTab = bar.newTab()
                    .setText("Расписание")
                    .setTabListener(
                            new TabListener<CurrentDayScheduleFragment>(getActivity(), "scheduleFragment", CurrentDayScheduleFragment.class));
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



    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {

        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                fragmentTransaction.add(R.id.fragment_main_page_single, mFragment, mTag);
            } else {
                fragmentTransaction.attach(mFragment);
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            if (mFragment != null) {
                fragmentTransaction.detach(mFragment);
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

        }
    }
}
