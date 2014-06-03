package com.despectra.android.journal.view.main_page;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.view.AbstractApiFragment;
import com.despectra.android.journal.view.customviews.TitledCard;
import org.json.JSONObject;

/**
 * Created by Dmitry on 22.05.14.
 */
public class WallFragment extends AbstractApiFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int WALL_LOADER_ID = 0;

    public static final String KEY_DO_WALL_LOAD = "wallLoad";
    public static final String KEY_WALL_LOAD_STATE = "wallLoading";

    private TitledCard mWallCard;
    private ListView mWallView;
    private SimpleCursorAdapter mWallAdapter;

    private Cursor mCursor;
    private boolean mWallLoading;
    private boolean mLoadWall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mLoadWall = savedInstanceState.getBoolean(KEY_DO_WALL_LOAD);
        } else {
            mLoadWall = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main_page_wall, container, false);

        mWallView = (ListView) v.findViewById(R.id.wall_list);
        mWallView.setEmptyView(v.findViewById(R.id.listview_empty_message));
        mWallCard = (TitledCard) v.findViewById(R.id.wall_card);
        mWallLoading = (savedInstanceState != null) ? savedInstanceState.getBoolean(KEY_WALL_LOAD_STATE) : false;
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWallAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.wall_item,
                mCursor,
                new String[]{ Contract.Events.FIELD_TEXT, Contract.Events.FIELD_DATETIME},
                new int[]{R.id.wall_item_content, R.id.wall_item_time},
                0);
        mWallView.setAdapter(mWallAdapter);

        getLoaderManager().restartLoader(WALL_LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
        if (mLoadWall) {
            updateWall();
            mWallLoading = true;
            mLoadWall = false;
        }
        updateWallState();
    }

    @Override
    public void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_DO_WALL_LOAD, mLoadWall);
        outState.putBoolean(KEY_WALL_LOAD_STATE, mWallLoading);
    }


    private void updateWall() {
        String token = PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        if (token.isEmpty()) {
            return;
        }
        mServiceHelperController.getAllEvents(token, ApiServiceHelper.PRIORITY_LOW);
        setWallStateLoading();
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
                showProgress();
            } else {
                hideProgress();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri baseUri;
        String[] projection;
        String orderBy;
        switch (id) {
            case WALL_LOADER_ID:
                baseUri = Contract.Events.URI;
                projection = new String[]{Contract.Events._ID + " AS _id", Contract.Events.FIELD_TEXT, Contract.Events.FIELD_DATETIME};
                orderBy = Contract.Events.FIELD_DATETIME + " DESC";
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
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mWallAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mWallAdapter.swapCursor(null);
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        switch (actionCode) {
            case APICodes.ACTION_GET_EVENTS:
                setWallStateIdle();
                break;
        }
    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        setWallStateIdle();
        ApiErrorResponder.respondDialog(getFragmentManager(), (JSONObject)response);
    }
}
