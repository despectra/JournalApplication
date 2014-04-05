package com.despectra.android.journal.Fragments;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.despectra.android.journal.Adapters.CurrentScheduleAdapter;
import com.despectra.android.journal.Data.MainProvider;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Views.TitledCard;

/**
 * Created by Dmirty on 17.02.14.
 */
public class MainPageFragment extends Fragment implements LoaderCallbacks<Cursor> {

    public static final int WALL_LOADER_ID = 0;
    public static final int SCHED_LOADER_ID = 1;

    public static final String KEY_WALL_LOAD_STATE = "wallLoading";

    private TitledCard mWallCard;
    private ListView mScheduleListView;
    private ListView mWallListView;
    private SimpleCursorAdapter mWallAdapter;
    private CurrentScheduleAdapter mScheduleAdapter;
    private Cursor mCursor;

    private boolean mWallLoading;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main_page, container, false);

        mScheduleListView = (ListView)v.findViewById(R.id.schedule_list);
        mWallListView = (ListView) v.findViewById(R.id.wall_list);
        mWallListView.setEmptyView(v.findViewById(R.id.listview_empty_message));
        mWallCard = (TitledCard) v.findViewById(R.id.wall_card);
        mWallLoading = (savedInstanceState != null) ? savedInstanceState.getBoolean(KEY_WALL_LOAD_STATE) : false;

        CurrentScheduleAdapter.Model[] schedule = generateMockSchedule();
        mScheduleAdapter = new CurrentScheduleAdapter(getActivity(), schedule);
        mScheduleListView.setAdapter(mScheduleAdapter);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWallAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.wall_item,
                mCursor,
                new String[]{ MainProvider.EVENTS_TEXT, MainProvider.EVENTS_DATETIME },
                new int[]{R.id.wall_item_content, R.id.wall_item_time},
                0);
        mWallListView.setAdapter(mWallAdapter);
        getLoaderManager().initLoader(WALL_LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWallState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WALL_LOAD_STATE, mWallLoading);
    }

    public void setWallStateLoading() {
        mWallLoading = true;
        updateWallState();
    }

    public void setWallStateIdle() {
        mWallLoading = false;
        updateWallState();
    }

    private void updateWallState() {
        if (mWallCard != null) {
            if (mWallLoading) {
                mWallCard.showSpinner();
            } else {
                mWallCard.hideSpinner();
            }
        }
    }

    private CurrentScheduleAdapter.Model[] generateMockSchedule() {
        CurrentScheduleAdapter.Model[] schedule = new CurrentScheduleAdapter.Model[5];
        schedule[0] = new CurrentScheduleAdapter.Model(
                CurrentScheduleAdapter.STATUS_PAST,
                "Класс 9'A'",
                "Алгебра",
                "8:30",
                "9:15",
                Color.rgb(140, 30, 1),
                Color.rgb(200, 155, 0)
                );
        schedule[1] = new CurrentScheduleAdapter.Model(
                CurrentScheduleAdapter.STATUS_CURRENT,
                "Класс 9'A'",
                "Алгебра",
                "8:30",
                "9:15",
                Color.rgb(140, 30, 1),
                Color.rgb(200, 155, 0)
        );
        schedule[2] = new CurrentScheduleAdapter.Model(
                CurrentScheduleAdapter.STATUS_FUTURE,
                "Класс 9'A'",
                "Алгебра",
                "8:30",
                "9:15",
                Color.rgb(140, 30, 1),
                Color.rgb(200, 155, 0)
        );
        schedule[3] = new CurrentScheduleAdapter.Model(
                CurrentScheduleAdapter.STATUS_FUTURE,
                "Класс 9'A'",
                "Алгебра",
                "8:30",
                "9:15",
                Color.rgb(140, 30, 1),
                Color.rgb(200, 155, 0)
        );
        schedule[4] = new CurrentScheduleAdapter.Model(
                CurrentScheduleAdapter.STATUS_FUTURE,
                "Класс 9'A'",
                "Алгебра",
                "8:30",
                "9:15",
                Color.rgb(140, 30, 1),
                Color.rgb(200, 155, 0)
        );
        return schedule;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri baseUri;
        String[] projection;
        String orderBy;
        switch (id) {
            case WALL_LOADER_ID:
                baseUri = MainProvider.EVENTS_URI;
                projection = new String[]{BaseColumns._ID, MainProvider.EVENTS_TEXT, MainProvider.EVENTS_DATETIME};
                orderBy = "datetime DESC";
                break;
            default:
                return null;
        }
        return new CursorLoader(
                getActivity(),
                baseUri,
                projection,
                null,
                null,
                orderBy
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> cursorLoader, Cursor cursor) {
        mWallAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> cursorLoader) {
        mWallAdapter.swapCursor(null);
    }

}
