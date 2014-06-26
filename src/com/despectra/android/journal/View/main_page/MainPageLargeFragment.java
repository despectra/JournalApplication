package com.despectra.android.journal.view.main_page;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
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
public class MainPageLargeFragment extends Fragment {
    public static final String FRAGMENT_TAG = "MainPageFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_page, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#eeeeee")));
        setHasOptionsMenu(false);
        WallFragment fstFragment = new WallFragment();
        CurrentDayScheduleFragment sndFragment = new CurrentDayScheduleFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_main_page_fst, fstFragment)
                .replace(R.id.fragment_main_page_snd, sndFragment)
                .commit();

        getActivity().setTitle("Главная");
    }
}
