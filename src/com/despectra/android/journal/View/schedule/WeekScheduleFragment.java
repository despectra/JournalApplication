package com.despectra.android.journal.view.schedule;

import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.net.WebApiServer;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.model.WeekScheduleItem;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.view.AbstractApiFragment;
import com.despectra.android.journal.view.RemoteIdsCursorAdapter;
import com.despectra.android.journal.view.SimpleRemoteIdsAdapter;
import org.json.JSONObject;

/**
 * Created by Dmitry on 19.06.14.
 */
public class WeekScheduleFragment extends AbstractApiFragment implements
        LoaderCallbacks<Cursor> {

    public static final int LOADER_GROUPS = 0;
    public static final int LOADER_SCHEDULE = 1;
    public static final String TAG = "WeekScheduleFragment";

    private GroupsFragment mGroupsFragment;
    private ScheduleFragment mSchedFragment;

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    private boolean mLoading;
    private String mToken;

    public void setGroupsFragment(GroupsFragment fragment) {
        mGroupsFragment = fragment;
    }

    public void setScheduleFragment(ScheduleFragment fragment) {
        mSchedFragment = fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pager_container, container, false);
        mPager = (ViewPager) view.findViewById(R.id.fragment_main_page_single);
        PagerTabStrip tabStrip = (PagerTabStrip) view.findViewById(R.id.pager_tab_strip);
        tabStrip.setTabIndicatorColorResource(android.R.color.holo_blue_dark);
        mPagerAdapter = new PagerAdapter(getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(1);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(LOADER_GROUPS, null, this);
        mLoading = savedInstanceState != null && savedInstanceState.getBoolean("load");
        mToken = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("load", mLoading);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mLoading) {
            mLoading = true;
            mServiceHelperController.getGroups(mToken, new EntityIds(0, 0), 0, 0, ApiServiceHelper.PRIORITY_HIGH);
            mServiceHelperController.getTeachers(mToken, 0, 0, ApiServiceHelper.PRIORITY_HIGH);
            mServiceHelperController.getSubjects(mToken, 0, 0, ApiServiceHelper.PRIORITY_HIGH);
            mServiceHelperController.getSubjectsOfAllTeachers(mToken, ApiServiceHelper.PRIORITY_LOW);
            mServiceHelperController.getGroupsOfAllTeachersSubjects(mToken, ApiServiceHelper.PRIORITY_LOW);
        }
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        if (actionCode == APICodes.ACTION_GET_GROUPS_OF_ALL_TS) {
            mServiceHelperController.getWeekScheduleForGroup(mToken,
                    mGroupsFragment.getGroupsAdapter().getSelectedEntityIdsByTable("groups"),
                    ApiServiceHelper.PRIORITY_LOW);
        }

        Log.e(TAG, WebApiServer.METHODS_MAP.get(actionCode));
        if (actionCode == APICodes.ACTION_GET_WEEK_SCHEDULE_FOR_GROUP) {
            Log.e(TAG, response.toString());
            getLoaderManager().restartLoader(LOADER_SCHEDULE, null, this);
        }
    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        ApiErrorResponder.respondDialog(getFragmentManager(), (JSONObject) response);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri;
        String[] projection;
        String selection;
        String[] selectionArgs;
        String orderBy;
        switch (i) {
            case LOADER_GROUPS:
                uri = Groups.URI;
                projection = new String[]{Groups._ID + " as _id", Groups.REMOTE_ID, Groups.FIELD_NAME, Groups.ENTITY_STATUS};
                selection = null;
                selectionArgs = null;
                orderBy = Groups.FIELD_NAME + " ASC";
                break;
            case LOADER_SCHEDULE:
                uri = Schedule.URI_FULL;
                projection = new String[]{Schedule._ID + " as _id", Schedule.REMOTE_ID, Schedule.FIELD_COLOR, Schedule.FIELD_DAY, Schedule.FIELD_LESSON_NUMBER,
                        Subjects.FIELD_NAME, Users.FIELD_LAST_NAME + "||" + Users.FIELD_FIRST_NAME + " as teacher"};
                selection = TSG.FIELD_GROUP_ID + " = ?";
                String groupId = String.valueOf(mGroupsFragment.getGroupsAdapter().getSelectedEntityIdsByTable("groups").getLocalId());
                selectionArgs = new String[]{groupId};

                orderBy = Schedule.FIELD_DAY + " ASC, " + Schedule.FIELD_LESSON_NUMBER + " ASC";
                break;
            default:
                return null;
        }
        return new CursorLoader(getActivity(), uri, projection, selection, selectionArgs, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case LOADER_GROUPS:
                mGroupsFragment.getGroupsAdapter().swapCursor(cursor);
                if (cursor.getCount() > 0) {
                    mGroupsFragment.getGroupsAdapter().setItemSelectedAtPos(0, cursor);
                    getLoaderManager().restartLoader(LOADER_SCHEDULE, null, this);
                }
                break;
            case LOADER_SCHEDULE:
                mSchedFragment.updateSchedule(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }


    public void onGroupItemClick(View itemView, int position, JoinedEntityIds ids) {
        getLoaderManager().restartLoader(LOADER_SCHEDULE, null, this);
        mServiceHelperController.getWeekScheduleForGroup(mToken, ids.getIdsByTable(Groups.TABLE), ApiServiceHelper.PRIORITY_LOW);
    }

    private static class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new WeekScheduleFragment.GroupsFragment();
                case 1:
                    return new ScheduleFragment();
                default:
                    return new Fragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public float getPageWidth(int position) {
            switch (position) {
                case 0:
                    return 0.3f;
                case 1:
                    return 1;
                default:
                    return 0.0f;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Класс";
                case 1:
                    return "Расписание";
                default:
                    return null;
            }
        }
    }

    public static class GroupsFragment extends Fragment implements RemoteIdsCursorAdapter.OnItemClickListener {
        private WeekScheduleFragment mParentFragment;
        private ListView mGroupsListView;
        private SimpleRemoteIdsAdapter mGroupsAdapter;
        private Cursor mCursor;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_simple_entities_list_frame, container, false);
            mGroupsListView = (ListView)view.findViewById(R.id.entities_list_view);
            ((FrameLayout)view).setForeground(getActivity().getResources().getDrawable(R.drawable.left_shadow));
            return view;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mParentFragment = (WeekScheduleFragment) getParentFragment();
            mParentFragment.setGroupsFragment(this);

            mGroupsListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            mGroupsAdapter = new SimpleRemoteIdsAdapter(getActivity(),
                    R.layout.item_simple_1,
                    mCursor,
                    new EntityIdsColumns[]{new EntityIdsColumns(Groups.TABLE, "_id", Groups.REMOTE_ID)},
                    Groups.ENTITY_STATUS,
                    new String[]{Groups.FIELD_NAME},
                    new int[]{R.id.text1});
            mGroupsAdapter.setOnItemClickListener(this);
            mGroupsListView.setAdapter(mGroupsAdapter);
        }

        public SimpleRemoteIdsAdapter getGroupsAdapter() {
            return mGroupsAdapter;
        }

        @Override
        public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
            mParentFragment.onGroupItemClick(itemView, position, ids);
        }
    }

    public static class ScheduleFragment extends Fragment {
        private WeekScheduleFragment mParentFragment;
        private WeekScheduleView mScheduleView;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_week_schedule, container, false);
            mScheduleView = (WeekScheduleView) view.findViewById(R.id.week_schedule_view);
            mScheduleView.setOnEventSelectedListener(new ScheduleRowAdapter.OnScheduleItemClickedListener() {
                @Override
                public void onItemClicked(int day, int lessonNum, WeekScheduleItem item) {
                    Toast.makeText(getActivity(), String.format("%d, %d", day, lessonNum), Toast.LENGTH_LONG).show();
                }
            });
            return view;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mParentFragment = (WeekScheduleFragment) getParentFragment();
            mParentFragment.setScheduleFragment(this);

        }

        public void updateSchedule(Cursor cursor) {
            mScheduleView.updateSchedule(cursor);
        }
    }
}
