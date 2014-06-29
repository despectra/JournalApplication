package com.despectra.android.journal.view.journal;

import android.app.LoaderManager;
import android.content.Intent;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.AbstractApiFragment;
import com.despectra.android.journal.view.RemoteIdsCursorAdapter;
import com.despectra.android.journal.view.SimpleRemoteIdsAdapter;

/**
 * Created by Dmitry on 28.06.14.
 */
public class JournalsListFragment extends AbstractApiFragment implements LoaderCallbacks<Cursor>, RemoteIdsCursorAdapter.OnItemClickListener {

    public static final String TAG = "JournalsListFragment";
    private ListView mJournalsView;
    private SimpleRemoteIdsAdapter mJournalsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simple_entities_list, container, false);
        mJournalsView = (ListView) view.findViewById(R.id.entities_list_view);
        int _30dp = Utils.dpToPx(getActivity(), 30);
        mJournalsView.setDividerHeight(_30dp);
        mJournalsView.setClipToPadding(false);
        mJournalsView.setPadding(_30dp, _30dp, _30dp, _30dp);
        mJournalsView.setDrawSelectorOnTop(true);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mJournalsAdapter = new SimpleRemoteIdsAdapter(getActivity(), R.layout.item_card_1, mCursor,
                new EntityIdsColumns[]{new EntityIdsColumns(Groups.TABLE, "_id", Groups.REMOTE_ID)},
                Groups.ENTITY_STATUS, new String[]{Groups.FIELD_NAME, "counts"}, new int[]{R.id.title, R.id.subtitle}, 0);
        mJournalsView.setAdapter(mJournalsAdapter);
        mJournalsAdapter.setOnItemClickListener(this);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Классные журналы");
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mLoading) {
            mLoading = true;
            mServiceHelperController.getAllGroups(mToken, new EntityIds(0, 0), ApiServiceHelper.PRIORITY_HIGH);
            mServiceHelperController.getAllSubjects(mToken, ApiServiceHelper.PRIORITY_HIGH);
            mServiceHelperController.getTeachers(mToken, 0, 0, ApiServiceHelper.PRIORITY_HIGH);
            mServiceHelperController.getSubjectsOfAllTeachers(mToken, ApiServiceHelper.PRIORITY_LOW);
            mServiceHelperController.getGroupsOfAllTeachersSubjects(mToken, ApiServiceHelper.PRIORITY_LOW);
        }
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {

    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(
                getActivity(),
                TSG.URI_WITH_GROUPS,
                new String[]{"DISTINCT " + TSG.FIELD_GROUP_ID + " as _id",
                        Groups.REMOTE_ID,
                        Groups.FIELD_NAME,
                        Groups.ENTITY_STATUS,
                        String.format("'Учителей: '|| COUNT (DISTINCT %s) ||' Предметов: '|| COUNT (DISTINCT %s) as counts",
                                TeachersSubjects.FIELD_TEACHER_ID, TeachersSubjects.FIELD_SUBJECT_ID)
                },
                String.format("NOT _id = 0 GROUP BY %s", TSG.FIELD_GROUP_ID),
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mJournalsAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
        EntityIds groupIds = ids.getIdsByTable(Groups.TABLE);
        Intent intent = new Intent(getActivity(), GroupsJournalActivity.class);
        intent.putExtra("groupIds", groupIds.toBundle());
        intent.putExtra("groupName", ((TextView) itemView.findViewById(R.id.title)).getText().toString());
        startActivity(intent);
    }
}
