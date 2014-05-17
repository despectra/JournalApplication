package com.despectra.android.journal.Fragments;

import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.ListView;
import com.despectra.android.journal.Adapters.RemoteIdCursorAdapter;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Dialogs.AddEditDialog;
import com.despectra.android.journal.Dialogs.AddEditGroupDialog;
import com.despectra.android.journal.Dialogs.SimpleConfirmationDialog;

/**
 * Created by Dmitry on 08.04.14.
 */
public abstract class EntitiesListFragment extends AbstractApiFragment implements LoaderCallbacks<Cursor>,
        RemoteIdCursorAdapter.OnItemCheckedListener,
        RemoteIdCursorAdapter.OnItemClickListener
{

    public static final int LOADER_MAIN = 0;

    public static final String KEY_ACTION_MODE_ON = "hasAMode";
    public static final String KEY_CHECKED_GROUPS_COUNT = "checkedGroupsCount";
    public static final String KEY_CHECKED_ENTITIES_LOCAL = "checkedLocal";
    public static final String KEY_CHECKED_ENTITIES_REMOTE = "checkedRemote";

    protected ListView mEntitiesListView;
    protected RemoteIdCursorAdapter mEntitiesAdapter;
    protected ActionMode mActionMode;
    protected AddEditDialog mEntityDialog;
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
        mEntitiesListView = (ListView) v.findViewById(getListViewId());
        setHasOptionsMenu(true);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
            long[] checkedEntitiesLocalIds = savedInstanceState.getLongArray(KEY_CHECKED_ENTITIES_LOCAL);
            long[] checkedEntitiesRemoteIds = savedInstanceState.getLongArray(KEY_CHECKED_ENTITIES_REMOTE);
            mEntitiesAdapter.setCheckedItemIdsAsArray(checkedEntitiesLocalIds, checkedEntitiesRemoteIds, false);
            mIsInActionMode = savedInstanceState.getBoolean(KEY_ACTION_MODE_ON);
            mCheckedEntitiesCount = savedInstanceState.getInt(KEY_CHECKED_GROUPS_COUNT);
            if (mIsInActionMode) {
                mActionMode = getActivity().startActionMode(mActionModeCallback);
            }
            SimpleConfirmationDialog confirmDeletingDialog = (SimpleConfirmationDialog) getFragmentManager().findFragmentByTag(getConfirmDelDialogTag());
            if (confirmDeletingDialog != null) {
                confirmDeletingDialog.setOnConfirmListener(getConfirmDelListener());
            }
            mEntityDialog = (AddEditGroupDialog) getFragmentManager().findFragmentByTag(getAddEditDialogTag());
            if (mEntityDialog != null) {
                mEntityDialog.setDialogListener(getAddEditDialogListener());
            }

        } else {
            mIsInActionMode = false;
            mCheckedEntitiesCount = 0;
        }
        getLoaderManager().initLoader(LOADER_MAIN, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
        if (mLoadEntities) {
            mLoadEntities = false;
            updateEntitiesList();
        } else {
            int runningCount = mServiceHelperController.getRunningActionsCount();
            notifyAboutRunningActions(runningCount);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_ACTION_MODE_ON, mIsInActionMode);
        outState.putInt(KEY_CHECKED_GROUPS_COUNT, mCheckedEntitiesCount);
        outState.putLongArray(KEY_CHECKED_ENTITIES_LOCAL, mEntitiesAdapter.getCheckedLocalIdsAsArray());
        outState.putLongArray(KEY_CHECKED_ENTITIES_REMOTE, mEntitiesAdapter.getCheckedRemoteIdsAsArray());
    }

    @Override
    public abstract Loader<Cursor> onCreateLoader(int id, Bundle bundle);

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mEntitiesAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mEntitiesAdapter.swapCursor(null);
    }

    @Override
    public void onItemChecked(long localId, long remoteId, int checkedCount, boolean checked) {
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

    @Override
    public abstract void onItemClick(View itemView, long localId, long remoteId);

    public abstract int getActionModeMenuRes();

    public abstract boolean onActionModeItemClicked(ActionMode actionMode, MenuItem menuItem);

    protected abstract RemoteIdCursorAdapter.OnItemPopupMenuListener getItemPopupMenuListener();

    protected abstract int getItemPopupMenuRes();

    protected abstract int getFragmentLayoutRes();

    protected abstract int getListViewId();

    protected abstract RemoteIdCursorAdapter getRemoteIdAdapter();

    protected abstract AddEditDialog.DialogButtonsListener getAddEditDialogListener();

    protected abstract String getAddEditDialogTag();

    protected abstract SimpleConfirmationDialog.OnConfirmListener getConfirmDelListener();

    protected abstract String getConfirmDelDialogTag();

    protected abstract void notifyAboutRunningActions(int runningCount);

    protected abstract void updateEntitiesList();
}
