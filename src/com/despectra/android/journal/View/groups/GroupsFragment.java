package com.despectra.android.journal.view.groups;

import android.support.v4.content.CursorLoader;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.*;
import android.widget.*;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.view.*;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.view.customviews.StatusBar;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Dmitry on 08.04.14.
 */

public class GroupsFragment extends EntitiesListFragment {
    public static final String FRAGMENT_TAG = "GroupsFragment";
    public static final String CONFIRM_DIALOG_TAG = "ConfirmDeleteGroups";

    private StatusBar mStatusBar;
    private AddEditSimpleItemDialog mAddEditDialog;

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
            JoinedEntityIds[] groupsAllIds = mEntitiesAdapter.getCheckedIds();
            EntityIds[] groupsIds = new EntityIds[groupsAllIds.length];
            for (int i = 0; i < groupsAllIds.length; i++) {
                groupsIds[i] = groupsAllIds[i].getIdsByTable(Contract.Groups.TABLE);
            }
            mServiceHelperController.deleteGroups(mToken, groupsIds, ApiServiceHelper.PRIORITY_LOW);
            if (mIsInActionMode) {
                mActionMode.finish();
            }
        }
    };

    private AddEditSimpleItemDialog.DialogListener mGroupDialogListener = new AddEditSimpleItemDialog.DialogListener() {
        @Override
        public void onAddItem(String name) {
            if (!mToken.isEmpty()) {
                int runningCount = mServiceHelperController.getRunningActionsCount();
                if (runningCount > 0) {
                    mStatusBar.setStatusText(String.format("Добавление класса %s. Выполняется: %d", name, runningCount));
                } else {
                    mStatusBar.showStatus("Добавление класса " + name);
                    mStatusBar.showSpinner();
                }
                mServiceHelperController.addGroup(mToken, name, new EntityIds(0, 0), ApiServiceHelper.PRIORITY_HIGH);
            }
        }

        @Override
        public void onEditItem(String name, EntityIds itemIds) {
            if (!mToken.isEmpty()) {
                int runningCount = mServiceHelperController.getRunningActionsCount();
                if (runningCount > 0) {
                    mStatusBar.setStatusText(String.format("Обновление класса %s. Выполняется: %d", name, runningCount));
                } else {
                    mStatusBar.showStatus("Обновление класса " + name);
                    mStatusBar.showSpinner();
                }
                mServiceHelperController.updateGroup(mToken, itemIds, name, new EntityIds(0, 0), ApiServiceHelper.PRIORITY_HIGH);
            }
        }
    };

    private MultipleRemoteIdsCursorAdapter.OnItemPopupMenuListener mGroupPopupListener = new MultipleRemoteIdsCursorAdapter.OnItemPopupMenuListener() {
        @Override
        public void onMenuItemSelected(MenuItem item, View adapterItemView, JoinedEntityIds ids) {
            int runningCount = mServiceHelperController.getRunningActionsCount();
            String status;
            switch (item.getItemId()) {
                case R.id.action_edit:
                    mAddEditDialog = (AddEditSimpleItemDialog) getFragmentManager().findFragmentByTag(AddEditSimpleItemDialog.FRAGMENT_TAG);
                    String groupName = ((TextView) adapterItemView.findViewById(R.id.text1)).getText().toString();
                    if (mAddEditDialog == null) {
                        mAddEditDialog = AddEditSimpleItemDialog.newInstance("Добавить класс", "Редактировать класс",
                                groupName, ids.getIdsByTable(Contract.Groups.TABLE));
                    }
                    mAddEditDialog.setDialogListener(mGroupDialogListener);
                    mAddEditDialog.showInMode(AddEditDialog.MODE_EDIT, getFragmentManager(), AddEditSimpleItemDialog.FRAGMENT_TAG);
                    break;
                case R.id.action_delete:
                    if (runningCount > 0) {
                        status = String.format("Удаление классов. Выполняется: %d", runningCount);
                    } else {
                        status = "Удаление классов";
                    }
                    mStatusBar.showStatus(status);
                    mStatusBar.showSpinner();
                    mServiceHelperController.deleteGroups(mToken, new EntityIds[]{ids.getIdsByTable(Contract.Groups.TABLE)}, ApiServiceHelper.PRIORITY_HIGH);
                    break;
                default:
                    return;
            }
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri baseUri;
        String[] projection;
        String orderBy;
        switch (id) {
            case LOADER_MAIN:
                baseUri = Contract.Groups.URI;
                projection = new String[]{Contract.Groups._ID + " AS _id",
                        Contract.Groups.Remote.REMOTE_ID,
                        Contract.Groups.FIELD_NAME,
                        Contract.Groups.FIELD_PARENT_ID,
                        Contract.Groups.ENTITY_STATUS};
                orderBy = Contract.Groups.FIELD_NAME + " ASC";
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
    public void onItemClick(View itemView, JoinedEntityIds ids) {
        String groupName = ((TextView)itemView.findViewById(R.id.text1)).getText().toString();
        Intent intent = new Intent(getActivity(), GroupActivity.class);
        intent.putExtra(GroupActivity.EXTRA_KEY_GROUP_IDS, ids.getIdsByTable(Contract.Groups.TABLE).toBundle());
        intent.putExtra(GroupActivity.EXTRA_KEY_GROUP_NAME, groupName);
        intent.putExtra(GroupActivity.EXTRA_KEY_IS_SUBGROUP, false);
        startActivity(intent);
    }

    @Override
    public int getActionModeMenuRes() {
        return R.menu.groups_fragment_cab_menu;
    }

    @Override
    public boolean onActionModeItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                SimpleConfirmationDialog confirmDialog = SimpleConfirmationDialog.newInstance(
                        "Удаление классов",
                        "Внимание! Удалятся не только классы, но и все связанные ученики, уроки, связи с учителями и прочее. Продолжить?");
                confirmDialog.setOnConfirmListener(getConfirmDelListener());
                confirmDialog.show(getFragmentManager(), CONFIRM_DIALOG_TAG);
                return true;
        }
        return true;
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter.OnItemPopupMenuListener getItemPopupMenuListener() {
        return mGroupPopupListener;
    }

    @Override
    protected int getItemPopupMenuRes() {
        return R.menu.item_edit_del_menu;
    }

    @Override
    protected int getFragmentLayoutRes() {
        return R.layout.fragment_groups;
    }

    @Override
    protected int getListViewId() {
        return R.id.entities_list_view;
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter getRemoteIdAdapter() {
        EntityIdsColumns[] columns = new EntityIdsColumns[]{
            new EntityIdsColumns(Contract.Groups.TABLE, "_id", Contract.Groups.Remote.REMOTE_ID)
        };
        return new MultipleRemoteIdsCursorAdapter(getActivity(),
                R.layout.item_checkable_1,
                mCursor,
                new String[]{Contract.Groups.FIELD_NAME},
                new int[]{R.id.text1},
                columns,
                Contract.Groups.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.dropdown_btn1,
                0);
    }

    @Override
    protected SimpleConfirmationDialog.OnConfirmListener getConfirmDelListener() {
        return mConfirmDeletingListener;
    }

    @Override
    protected String getConfirmDelDialogTag() {
        return CONFIRM_DIALOG_TAG;
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
                if (mAddEditDialog == null) {
                    mAddEditDialog = AddEditSimpleItemDialog.newInstance("Добавить класс", "Редактировать класс", "", new EntityIds(-1, -1));
                }
                mAddEditDialog.setDialogListener(mGroupDialogListener);
                mAddEditDialog.showInMode(AddEditDialog.MODE_ADD, getFragmentManager(), AddEditSimpleItemDialog.FRAGMENT_TAG);
                break;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mStatusBar = (StatusBar) view.findViewById(R.id.groups_status_bar);
        return view;
    }

    @Override
    protected void restoreCustom() {
        mAddEditDialog = (AddEditSimpleItemDialog) getFragmentManager().findFragmentByTag(mAddEditDialog.FRAGMENT_TAG);
        if (mAddEditDialog != null) {
            mAddEditDialog.setDialogListener(mGroupDialogListener);
        }
    }

    @Override
    protected void notifyAboutRunningActions(int runningCount) {
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

    @Override
    protected void updateEntitiesList() {
        mStatusBar.showSpinner();
        mStatusBar.showStatus("Обновление списка классов");
        mServiceHelperController.getAllGroups(mToken, new EntityIds(0, 0), ApiServiceHelper.PRIORITY_LOW);
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
                    getLoaderManager().restartLoader(LOADER_MAIN, null, this);
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
            JSONObject jsonResponse = (JSONObject) response;
            try {
                mStatusBar.setStatusText(jsonResponse.getString("error_message"));
            } catch (JSONException e) {
            }
        }
    }
}