package com.despectra.android.journal.Fragments;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.despectra.android.journal.R;

/**
 * Created by Dmitry on 07.04.14.
 */
public class StaffFragment extends AbstractApiFragment {

    public static final String FRAGMENT_TAG = "StaffFragment";

    public StaffFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.staff_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {

    }
}
