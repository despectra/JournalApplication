package com.despectra.android.journal.view;

import android.app.ActionBar;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import android.widget.ListView;
import android.widget.TextView;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.BasicClientHelperController;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONObject;

/**
 * Created by Dmitry on 08.04.14.
 */
public abstract class EntitiesListFragment extends AbstractApiFragment implements LoaderCallbacks<Cursor>,
        MultipleRemoteIdsCursorAdapter.OnItemCheckedListener,
        MultipleRemoteIdsCursorAdapter.OnItemClickListener
{

    public static final int LOADER_MAIN = 0;

    public static final String KEY_ACTION_MODE_ON = "hasAMode";
    public static final String KEY_CHECKED_GROUPS_COUNT = "checkedGroupsCount";
    public static final String KEY_CHECKED_ENTITIES = "checked";

    protected ListView mEntitiesListView;
    protected TextView mEmptyListNotificator;
    protected MultipleRemoteIdsCursorAdapter mEntitiesAdapter;
    protected ActionMode mActionMode;
    protected Cursor mCursor;

    protected String mToken;
    protected int mCheckedEntitiesCount;
    protected boolean mLoadEntities;
    protected boolean mIsInActionMode;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mIsInActionMode = true;
            actionMode.setTitle(String.format("Выбрано: %d", mCheckedEntitiesCount));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(EntitiesListFragment.this.getActionModeMenuRes(), menu);
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            return EntitiesListFragment.this.onActionModeItemClicked(actionMode, menuItem);
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mIsInActionMode = false;
            mCheckedEntitiesCount = 0;
            mEntitiesAdapter.setCheckedNone();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(getFragmentLayoutRes(), container, false);
        mEntitiesListView = (ListView) v.findViewById(R.id.entities_list_view);
        mEmptyListNotificator = (TextView)View.inflate(getActivity(), R.layout.empty_list_notification, null);
        mEmptyListNotificator.setText(getEmptyListMessage());
        mEntitiesListView.setEmptyView(mEmptyListNotificator);
        setHasOptionsMenu(true);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mToken = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        if (mToken.isEmpty()) {
            throw new IllegalStateException("Current session token not found");
        }

        mEntitiesAdapter = getRemoteIdAdapter();
        mEntitiesAdapter.setOnItemCheckedListener(this);
        mEntitiesAdapter.setOnItemClickListener(this);
        mEntitiesAdapter.setItemPopupMenu(getItemPopupMenuRes(), getItemPopupMenuListener());
        mEntitiesListView.setAdapter(mEntitiesAdapter);

        mLoadEntities = (savedInstanceState == null);
        if (savedInstanceState != null) {
            //JoinedEntityIds[] checkedEntities = (JoinedEntityIds[])savedInstanceState.getParcelableArray(KEY_CHECKED_ENTITIES);

            mEntitiesAdapter.restoreCheckedItems(savedInstanceState.getBundle(KEY_CHECKED_ENTITIES), false);
            mIsInActionMode = savedInstanceState.getBoolean(KEY_ACTION_MODE_ON);
            mCheckedEntitiesCount = savedInstanceState.getInt(KEY_CHECKED_GROUPS_COUNT);
            if (mIsInActionMode) {
                mActionMode = getActivity().startActionMode(mActionModeCallback);
            }
            SimpleConfirmationDialog confirmDeletingDialog = (SimpleConfirmationDialog) getFragmentManager().findFragmentByTag(getConfirmDelDialogTag());
            if (confirmDeletingDialog != null) {
                confirmDeletingDialog.setOnConfirmListener(getConfirmDelListener());
            }

            restoreCustom();

        } else {
            mIsInActionMode = false;
            mCheckedEntitiesCount = 0;
        }
        getLoaderManager().initLoader(LOADER_MAIN, null, this);
        String title = getTitle();
        if (title != null) {
            getActivity().setTitle(title);
        }
    }

    protected abstract String getTitle();

    protected abstract void restoreCustom();

    @Override
    public void onResume() {
        super.onResume();
        if (mLoadEntities) {
            mLoadEntities = false;
            updateEntitiesList();
        } else {
            int runningCount = mServiceHelperController.getRunningActionsCount();
            notifyAboutRunningActions(runningCount);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_ACTION_MODE_ON, mIsInActionMode);
        outState.putInt(KEY_CHECKED_GROUPS_COUNT, mCheckedEntitiesCount);
        outState.putBundle(KEY_CHECKED_ENTITIES, mEntitiesAdapter.getCheckedItems());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mEntitiesAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mEntitiesAdapter.swapCursor(null);
    }

    @Override
    public void onItemChecked(JoinedEntityIds ids, int checkedCount, boolean checked) {
        mCheckedEntitiesCount = checkedCount;
        if (mCheckedEntitiesCount == 1) {
            if (checked) {
                mActionMode = getActivity().startActionMode(mActionModeCallback);
            } else {
                mActionMode.setTitle(String.format("Выбрано: %d", mCheckedEntitiesCount));
            }
        } else if (checkedCount > 1) {
            mActionMode.setTitle(String.format("Выбрано: %d", mCheckedEntitiesCount));
        } else {
            mActionMode.finish();
        }

    }

    protected abstract String getEmptyListMessage();

    @Override
    public abstract void onItemClick(View itemView, int position, JoinedEntityIds ids);

    public abstract int getActionModeMenuRes();

    public abstract boolean onActionModeItemClicked(ActionMode actionMode, MenuItem menuItem);

    protected abstract MultipleRemoteIdsCursorAdapter.OnItemPopupMenuListener getItemPopupMenuListener();

    protected abstract int getItemPopupMenuRes();

    protected abstract int getFragmentLayoutRes();

    protected abstract int getListViewId();

    protected abstract MultipleRemoteIdsCursorAdapter getRemoteIdAdapter();

    protected abstract SimpleConfirmationDialog.OnConfirmListener getConfirmDelListener();

    protected abstract String getConfirmDelDialogTag();

    protected abstract void notifyAboutRunningActions(int runningCount);

    protected abstract void updateEntitiesList();
}
