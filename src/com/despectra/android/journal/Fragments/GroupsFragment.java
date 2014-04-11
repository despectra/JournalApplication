package com.despectra.android.journal.Fragments;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.view.*;
import android.widget.*;
import com.despectra.android.journal.Adapters.RemoteIdCursorAdapter;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Data.Contract;
import com.despectra.android.journal.Dialogs.AddEditDialog;
import com.despectra.android.journal.Dialogs.AddEditGroupDialog;
import com.despectra.android.journal.Dialogs.SimpleConfirmationDialog;
import com.despectra.android.journal.Dialogs.SimpleInfoDialog;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Services.ApiServiceHelper;
import com.despectra.android.journal.Views.StatusBar;

/**
 * Created by Dmitry on 08.04.14.
 */
public class GroupsFragment extends AbstractApiFragment implements LoaderCallbacks<Cursor>,RemoteIdCursorAdapter.OnItemCheckedListener {

    public static final int LOADER_GROUPS = 0;
    public static final String FRAGMENT_TAG = "GroupsFragment";
    public static final String CONFIRM_DIALOG_TAG = "ConfirmDeleteGroups";
    public static final String[] BASIC_ITEM_ACTIONS = {"Редактировать", "Удалить"};
    public static final int ITEM_ACTION_EDIT = 0;
    public static final int ITEM_ACTION_DELETE = 1;

    public static final String KEY_ACTION_MODE_ON = "hasAMode";
    public static final String KEY_CHECKED_GROUPS_COUNT = "checkedGroupsCount";
    public static final String KEY_CHECKED_GROUPS_LOCAL = "checkedLocal";
    public static final String KEY_CHECKED_GROUPS_REMOTE = "checkedRemote";

    private ListView mGroupsListView;
    private StatusBar mStatusBar;
    private RemoteIdCursorAdapter mGroupsAdapter;
    private ActionMode mActionMode;
    private AddEditGroupDialog mGroupDialog;
    private SimpleInfoDialog mTestDialog;
    private Cursor mCursor;

    private String mToken;
    private int mCheckedGroupsCount;
    private boolean mLoadGroups;
    private boolean mIsInActionMode;

    private AddEditDialog.DialogButtonsAdapter mAddEditGroupCallback = new AddEditDialog.DialogButtonsAdapter() {
        @Override
        public void onPositiveClicked(int mode, Object... args) {
            String name = (String) args[0];
            long groupId = (Long) args[1];
            switch (mode) {
                case AddEditDialog.MODE_ADD:
                    if (!mToken.isEmpty()) {
                        int runningCount = mServiceHelperController.getRunningActionsCount();
                        if (runningCount > 0) {
                            mStatusBar.setStatusText(String.format("Добавление класса %s. Выполняется: %d", name, runningCount));
                        } else {
                            mStatusBar.showStatus("Добавление класса " + name);
                            mStatusBar.showSpinner();
                        }
                        mServiceHelperController.addGroup(mToken, name, 0, ApiServiceHelper.PRIORITY_HIGH);
                    }
                    break;
                case AddEditDialog.MODE_EDIT:
                    if (!mToken.isEmpty()) {
                        int runningCount = mServiceHelperController.getRunningActionsCount();
                        if (runningCount > 0) {
                            mStatusBar.setStatusText(String.format("Обновление класса %s. Выполняется: %d", name, runningCount));
                        } else {
                            mStatusBar.showStatus("Обновление класса " + name);
                            mStatusBar.showSpinner();
                        }
                        mServiceHelperController.updateGroup(mToken, groupId, name, 0, ApiServiceHelper.PRIORITY_HIGH);
                    }
                    break;
            }
        }
    };

    private SimpleConfirmationDialog.OnConfirmListener mConfirmDeletingListener = new SimpleConfirmationDialog.OnConfirmListener() {
        @Override
        public void onConfirm() {
            int runningCount = mServiceHelperController.getRunningActionsCount();
            if (runningCount > 0) {
                mStatusBar.setStatusText(String.format("Удаление классов. Выполняется: %d", runningCount));
            } else {
                mStatusBar.showStatus("Удаление классов");
                mStatusBar.showSpinner();
            }
            mServiceHelperController.deleteGroups(mToken, mGroupsAdapter.getCheckedRemoteIdsAsArray(), ApiServiceHelper.PRIORITY_LOW);
            if (mIsInActionMode) {
                mActionMode.finish();
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
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_delete:
                    SimpleConfirmationDialog confirmDialog = SimpleConfirmationDialog.newInstance(
                            "Удаление классов",
                            "Внимание! Удалятся не только классы, но и все связанные ученики, уроки, связи с учителями и прочее. Продолжить?");
                    confirmDialog.setOnConfirmListener(mConfirmDeletingListener);
                    confirmDialog.show(getFragmentManager(), CONFIRM_DIALOG_TAG);
                    return true;
            }
            return true;
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
        public void onMenuItemSelected(MenuItem item, View adapterItemView, long listItemLocalId, long listItemRemoteId) {
            int runningCount = mServiceHelperController.getRunningActionsCount();
            String status;
            switch (item.getItemId()) {
                case R.id.action_edit:
                    mGroupDialog = (AddEditGroupDialog) getFragmentManager().findFragmentByTag(AddEditGroupDialog.FRAGMENT_TAG);
                    String groupName = ((TextView) adapterItemView.findViewById(R.id.text1)).getText().toString();
                    if (mGroupDialog == null) {
                        mGroupDialog = AddEditGroupDialog.newInstance("Добавление класса", "Редактирование класса", groupName, listItemRemoteId);
                    }
                    mGroupDialog.setDialogListener(mAddEditGroupCallback);
                    mGroupDialog.setGroupId(listItemRemoteId);
                    mGroupDialog.setGroupText(groupName);
                    mGroupDialog.showInMode(AddEditDialog.MODE_EDIT, getFragmentManager(), AddEditGroupDialog.FRAGMENT_TAG);
                    break;
                case R.id.action_delete:
                    if (runningCount > 0) {
                        status = String.format("Удаление классов. Выполняется: %d", runningCount);
                    } else {
                        status = "Удаление классов";
                    }
                    mStatusBar.showStatus(status);
                    mStatusBar.showSpinner();
                    mServiceHelperController.deleteGroups(mToken, new long[]{listItemRemoteId}, ApiServiceHelper.PRIORITY_HIGH);
                    break;
                default:
                    return;
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
            long[] checkedGroupsLocal = savedInstanceState.getLongArray(KEY_CHECKED_GROUPS_LOCAL);
            long[] checkedGroupsRemote = savedInstanceState.getLongArray(KEY_CHECKED_GROUPS_REMOTE);
            mGroupsAdapter.setCheckedItemIdsAsArray(checkedGroupsLocal, checkedGroupsRemote, false);
            mIsInActionMode = savedInstanceState.getBoolean(KEY_ACTION_MODE_ON);
            mCheckedGroupsCount = savedInstanceState.getInt(KEY_CHECKED_GROUPS_COUNT);
            if (mIsInActionMode) {
                mActionMode = getActivity().startActionMode(mActionModeCallback);
            }
            SimpleConfirmationDialog confirmDeletingDialog = (SimpleConfirmationDialog) getFragmentManager().findFragmentByTag(CONFIRM_DIALOG_TAG);
            if (confirmDeletingDialog != null) {
                confirmDeletingDialog.setOnConfirmListener(mConfirmDeletingListener);
            }
            mGroupDialog = (AddEditGroupDialog) getFragmentManager().findFragmentByTag(AddEditGroupDialog.FRAGMENT_TAG);
            if (mGroupDialog != null) {
                mGroupDialog.setDialogListener(mAddEditGroupCallback);
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
            mServiceHelperController.getAllGroups(mToken, 0, ApiServiceHelper.PRIORITY_LOW);
        } else {
            int runningCount = mServiceHelperController.getRunningActionsCount();
            if (runningCount == 1) {
                switch (mServiceHelperController.getLastRunningActionCode()) {
                    case APICodes.ACTION_ADD_GROUP:
                        mStatusBar.showStatus("Добавление класса");
                        mStatusBar.showSpinner();
                        break;
                    case APICodes.ACTION_GET_GROUPS:
                        mStatusBar.showStatus("Обновление списка классов");
                        mStatusBar.showSpinner();
                        break;
                    case APICodes.ACTION_DELETE_GROUPS:
                        mStatusBar.showStatus("Удаление классов");
                        mStatusBar.showSpinner();
                }
            } else if(runningCount > 1) {
                mStatusBar.showStatus("Выполняемые запросы: " + runningCount);
                mStatusBar.showSpinner();
            } else {
                mStatusBar.hideStatus();
                mStatusBar.hideSpinner();
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
        outState.putLongArray(KEY_CHECKED_GROUPS_LOCAL, mGroupsAdapter.getCheckedLocalIdsAsArray());
        outState.putLongArray(KEY_CHECKED_GROUPS_REMOTE, mGroupsAdapter.getCheckedRemoteIdsAsArray());
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
                    mGroupDialog = AddEditGroupDialog.newInstance("Добавить класс", "Редактировать класс", "", -1);
                }
                mGroupDialog.setDialogListener(mAddEditGroupCallback);
                mGroupDialog.showInMode(AddEditDialog.MODE_ADD, getFragmentManager(), AddEditGroupDialog.FRAGMENT_TAG);
                break;
        }
        return true;
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {
        if (mStatusBar == null) {
            return;
        }
        if (actionCode != -1) {
            if (remainingActions > 0) {
                mStatusBar.setStatusText("Выполняемые запросы: " + remainingActions);
                return;
            }
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
                case APICodes.ACTION_DELETE_GROUPS:
                    mStatusBar.hideSpinner();
                    mStatusBar.showStatusThenHide("Удалено", 1500);
                    break;
                case APICodes.ACTION_UPDATE_GROUP:
                    mStatusBar.hideSpinner();
                    mStatusBar.showStatusThenHide("Инфрмация о классах обновлена", 1500);
            }
        } else {
            mStatusBar.hideSpinner();
            mStatusBar.setStatusText((String) response);
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
