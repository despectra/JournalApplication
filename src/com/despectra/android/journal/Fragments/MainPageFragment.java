package com.despectra.android.journal.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.despectra.android.journal.Adapters.CurrentScheduleAdapter;
import com.despectra.android.journal.R;

/**
 * Created by Dmirty on 17.02.14.
 */
public class MainPageFragment extends Fragment {
    private ListView mScheduleListView;
    private ListView mWallListView;
    private CurrentScheduleAdapter mScheduleAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main_page, container, false);

        mScheduleListView = (ListView)v.findViewById(R.id.schedule_list);
        mWallListView = (ListView) v.findViewById(R.id.wall_list);
        mWallListView.setEmptyView(v.findViewById(R.id.listview_empty_message));


        CurrentScheduleAdapter.Model[] schedule = generateMockSchedule();
        mScheduleAdapter = new CurrentScheduleAdapter(getActivity(), schedule);
        mScheduleListView.setAdapter(mScheduleAdapter);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
}
