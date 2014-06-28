package com.despectra.android.journal.view.groups;

import android.support.v4.content.CursorLoader;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.view.*;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.view.customviews.StatusBar;
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
            showProgress();

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
                showProgress();
                mServiceHelperController.addGroup(mToken, name, new EntityIds(0, 0), ApiServiceHelper.PRIORITY_HIGH);
            }
        }

        @Override
        public void onEditItem(String name, EntityIds itemIds) {
            if (!mToken.isEmpty()) {
                showProgress();
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
                    showProgress();
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
                        Contract.Groups.REMOTE_ID,
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
    protected String getEmptyListMessage() {
        return "Групп нет. Добавьте их с помощью кнопки на панели действий";
    }

    @Override
    public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
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
        return R.layout.fragment_simple_entities_list;
    }

    @Override
    protected int getListViewId() {
        return R.id.entities_list_view;
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter getRemoteIdAdapter() {
        EntityIdsColumns[] columns = new EntityIdsColumns[]{
            new EntityIdsColumns(Contract.Groups.TABLE, "_id", Contract.Groups.REMOTE_ID)
        };
        return new MultipleRemoteIdsCursorAdapter(getActivity(),
                R.layout.item_checkable_1,
                mCursor,
                new String[]{Contract.Groups.FIELD_NAME},
                new int[]{R.id.text1},
                columns,
                Contract.Groups.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.item_popup_menu_btn1
        );
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
        return view;
    }

    @Override
    protected String getTitle() {
        return "Классы";
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
    }

    @Override
    protected void updateEntitiesList() {
        showProgress();
        mServiceHelperController.getAllGroups(mToken, new EntityIds(0, 0), ApiServiceHelper.PRIORITY_LOW);
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        switch (actionCode) {
            case APICodes.ACTION_ADD_GROUP:
                getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                break;
        }
    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        ApiErrorResponder.respondDialog(getFragmentManager(), (JSONObject)response);
    }


}