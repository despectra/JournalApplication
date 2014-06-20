package com.despectra.android.journal.view.schedule;

import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.logic.services.ApiService;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.view.AbstractApiFragment;
import com.despectra.android.journal.view.SimpleRemoteIdsAdapter;
import org.json.JSONObject;

/**
 * Created by Dmitry on 19.06.14.
 */
public class WeekScheduleFragment extends AbstractApiFragment implements LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    public static final int LOADER_GROUPS = 0;
    public static final int LOADER_SCHEDULE = 1;
    public static final String TAG = "WeekScheduleFragment";

    private long mSelectedGroupLocalId;
    private ListView mGroupsListView;
    private SimpleRemoteIdsAdapter mGroupsAdapter;
    private WeekScheduleView mWeekScheduleView;
    private Cursor mCursor;
    private boolean mLoading;
    private String mToken;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_week_schedule, container, false);
        mGroupsListView = (ListView) view.findViewById(R.id.groups_list_view);
        mWeekScheduleView = (WeekScheduleView) view.findViewById(R.id.week_schedule_view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGroupsListView.setOnItemClickListener(this);
        mGroupsAdapter = new SimpleRemoteIdsAdapter(getActivity(),
                android.R.layout.simple_list_item_1,
                mCursor,
                new EntityIdsColumns[]{new EntityIdsColumns(Groups.TABLE, "_id", Groups.REMOTE_ID)},
                Groups.ENTITY_STATUS,
                new String[]{Groups.FIELD_NAME},
                new int[]{android.R.id.text1});
        mGroupsListView.setAdapter(mGroupsAdapter);
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
                projection = new String[]{"_id", Schedule.REMOTE_ID, Schedule.FIELD_COLOR, Schedule.FIELD_DAY, Schedule.FIELD_LESSON_NUMBER,
                        Subjects.FIELD_NAME, Users.FIELD_SURNAME + "||" + Users.FIELD_NAME + " as teacher"};
                selection = TSG.FIELD_GROUP_ID + " = ?";
                selectionArgs = new String[]{String.valueOf(mSelectedGroupLocalId)};
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
                mGroupsAdapter.swapCursor(cursor);
                break;
            case LOADER_SCHEDULE:
                mWeekScheduleView.updateSchedule(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
        mSelectedGroupLocalId = id;
        getLoaderManager().restartLoader(LOADER_SCHEDULE, null, this);
    }
}
