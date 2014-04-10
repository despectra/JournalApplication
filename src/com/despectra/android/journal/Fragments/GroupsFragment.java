package com.despectra.android.journal.Fragments;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.view.*;
import android.widget.*;
import com.despectra.android.journal.Adapters.RemoteIdCursorAdapter;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Data.Contract;
import com.despectra.android.journal.Data.MainProvider;
import com.despectra.android.journal.Dialogs.AddGroupDialog;
import com.despectra.android.journal.Dialogs.SimpleInfoDialog;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Views.BottomTabWidget;
import com.despectra.android.journal.Views.StatusBar;

import java.util.Arrays;

/**
 * Created by Dmitry on 08.04.14.
 */
public class GroupsFragment extends AbstractApiFragment implements LoaderCallbacks<Cursor>,RemoteIdCursorAdapter.OnItemCheckedListener {

    public static final int LOADER_GROUPS = 0;
    public static final String FRAGMENT_TAG = "GroupsFragment";
    public static final String[] BASIC_ITEM_ACTIONS = {"Редактировать", "Удалить"};
    public static final int ITEM_ACTION_EDIT = 0;
    public static final int ITEM_ACTION_DELETE = 1;

    public static final String KEY_ACTION_MODE_ON = "hasAMode";
    public static final String KEY_CHECKED_GROUPS_COUNT = "checkedGroupsCount";
    public static final String KEY_CHECKED_GROUPS_ARRAY = "checkedGroupsLongArr";

    private ListView mGroupsListView;
    private StatusBar mStatusBar;
    private RemoteIdCursorAdapter mGroupsAdapter;
    private ActionMode mActionMode;
    private AddGroupDialog mGroupDialog;
    private SimpleInfoDialog mTestDialog;
    private Cursor mCursor;

    private String mToken;
    private int mCheckedGroupsCount;
    private boolean mLoadGroups;
    private boolean mIsInActionMode;

    private AddGroupDialog.PositiveClickListener mAddGroupCallback = new AddGroupDialog.PositiveClickListener() {
        @Override
        public void onAddGroup(String name) {
            if (!mToken.isEmpty()) {
                mServiceHelperController.addGroup(mToken, name, 0);
                mStatusBar.showSpinner();
                mStatusBar.showStatus("Добавление класса " + name);
            }
        }
    };
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mIsInActionMode = true;
            actionMode.setTitle(String.format("Выбрано: %d", mCheckedGroupsCount));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.groups_fragment_cab_menu, menu);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mIsInActionMode = false;
            mCheckedGroupsCount = 0;
            mGroupsAdapter.setCheckedNone();
        }
    };

    private RemoteIdCursorAdapter.OnItemPopupMenuListener mGroupPopupListener = new RemoteIdCursorAdapter.OnItemPopupMenuListener() {
        @Override
        public void onMenuItemSelected(MenuItem item, long listItemLocalId, long listItemRemoteId) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    break;
                case R.id.action_delete:

                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_groups, container, false);
        mGroupsListView = (ListView) v.findViewById(R.id.groups_list_view);
        mStatusBar = (StatusBar) v.findViewById(R.id.groups_status_bar);
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
        mGroupsAdapter = new RemoteIdCursorAdapter(getActivity(),
                R.layout.item_checkable_1,
                mCursor,
                new String[]{Contract.Groups.FIELD_NAME},
                new int[]{R.id.text1},
                R.id.checkbox1,
                R.id.dropdown_btn1,
                0);
        mGroupsAdapter.setOnItemCheckedListener(this);
        mGroupsAdapter.setItemPopupMenu(R.menu.item_edit_del_menu, mGroupPopupListener);
        mGroupsListView.setAdapter(mGroupsAdapter);
        mLoadGroups = (savedInstanceState == null);
        if (savedInstanceState != null) {
            long[] checkedGroups = savedInstanceState.getLongArray(KEY_CHECKED_GROUPS_ARRAY);
            mGroupsAdapter.setCheckedItemIdsAsArray(checkedGroups, false);
            mIsInActionMode = savedInstanceState.getBoolean(KEY_ACTION_MODE_ON);
            mCheckedGroupsCount = savedInstanceState.getInt(KEY_CHECKED_GROUPS_COUNT);
            if (mIsInActionMode) {
                mActionMode = getActivity().startActionMode(mActionModeCallback);
            }
        } else {
            mIsInActionMode = false;
            mCheckedGroupsCount = 0;
        }
        getLoaderManager().restartLoader(LOADER_GROUPS, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
        if (mLoadGroups) {
            mLoadGroups = false;
            mStatusBar.showSpinner();
            mStatusBar.showStatus("Обновление списка классов");
            mServiceHelperController.getAllGroups(mToken, 0);
        } else {
            int runningActionCode = mServiceHelperController.hasRunningAction();
            if (runningActionCode != -1) {
                switch (runningActionCode) {
                    case APICodes.ACTION_ADD_GROUP:
                    case APICodes.ACTION_GET_GROUPS:
                        mStatusBar.showStatus("Обновление списка классов");
                        mStatusBar.showSpinner();
                        break;
                }
            }
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
        outState.putInt(KEY_CHECKED_GROUPS_COUNT, mCheckedGroupsCount);
        outState.putLongArray(KEY_CHECKED_GROUPS_ARRAY, mGroupsAdapter.getCheckedItemIdsAsArray());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.groups_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_group_add:
                if (mGroupDialog == null) {
                    mGroupDialog = new AddGroupDialog();
                }
                mGroupDialog.setPositiveCLickListener(mAddGroupCallback);
                mGroupDialog.show(getFragmentManager(), AddGroupDialog.FRAGMENT_TAG);
                break;
        }
        return true;
    }

    @Override
    public void onResponse(int actionCode, Object response) {
        if (actionCode != -1) {
            switch (actionCode) {
                case APICodes.ACTION_GET_GROUPS:
                    mStatusBar.hideSpinner();
                    mStatusBar.hideStatus();
                    break;
                case APICodes.ACTION_ADD_GROUP:
                    mStatusBar.hideSpinner();
                    mStatusBar.showStatusThenHide("Класс добавлен", 1500);
                    getLoaderManager().restartLoader(LOADER_GROUPS, null, this);
                    break;
            }
        } else {
            mStatusBar.hideSpinner();
            mStatusBar.setStatusText((String)response);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri baseUri;
        String[] projection;
        String orderBy;
        switch (id) {
            case LOADER_GROUPS:
                baseUri = Contract.Groups.URI;
                projection = new String[]{"*"};
                orderBy = null;
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
        mGroupsAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mGroupsAdapter.swapCursor(null);
    }

    @Override
    public void onItemChecked(long localId, long remoteId, int checkedCount, boolean checked) {
        mCheckedGroupsCount = checkedCount;
        if (mCheckedGroupsCount == 1) {
            if (checked) {
                mActionMode = getActivity().startActionMode(mActionModeCallback);
            } else {
                mActionMode.setTitle(String.format("Выбрано: %d", mCheckedGroupsCount));
            }
        } else if (checkedCount > 1) {
            mActionMode.setTitle(String.format("Выбрано: %d", mCheckedGroupsCount));
        } else {
            mActionMode.finish();
        }

    }
}
