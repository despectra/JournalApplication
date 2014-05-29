package com.despectra.android.journal.view.subjects;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.view.*;
import com.despectra.android.journal.view.groups.AddEditSimpleItemDialog;

/**
 * Created by Dmitry on 18.05.14.
 */
public class SubjectsFragment extends EntitiesListFragment {
    public static final String FRAGMENT_TAG = "SubjectsFragment";
    public static final String CONFIRM_DIALOG_TAG = "ConfirmDeleteSubjects";

    public AddEditSimpleItemDialog mAddEditDialog;

    private SimpleConfirmationDialog.OnConfirmListener mConfirmDeletingListener = new SimpleConfirmationDialog.OnConfirmListener() {
        @Override
        public void onConfirm() {
            getHostActivity().showProgressBar();

            mServiceHelperController.deleteSubjects(mToken,
                    mEntitiesAdapter.getCheckedLocalIdsAsArray(),
                    mEntitiesAdapter.getCheckedRemoteIdsAsArray(),
                    ApiServiceHelper.PRIORITY_LOW);
            if (mIsInActionMode) {
                mActionMode.finish();
            }
        }
    };

    private AddEditSimpleItemDialog.DialogListener mSubjectDialogListener = new AddEditSimpleItemDialog.DialogListener() {
        @Override
        public void onAddItem(String name) {
            if (!mToken.isEmpty()) {
                getHostActivity().showProgressBar();
                mServiceHelperController.addSubject(mToken, name, ApiServiceHelper.PRIORITY_HIGH);
            }
        }

        @Override
        public void onEditItem(String name, long localId, long remoteId) {
            if (!mToken.isEmpty()) {
                getHostActivity().showProgressBar();
                mServiceHelperController.updateSubject(mToken, localId, remoteId, name, ApiServiceHelper.PRIORITY_HIGH);
            }
        }
    };

    private RemoteIdCursorAdapter.OnItemPopupMenuListener mGroupPopupListener = new RemoteIdCursorAdapter.OnItemPopupMenuListener() {
        @Override
        public void onMenuItemSelected(MenuItem item, View adapterItemView, long listItemLocalId, long listItemRemoteId) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    mAddEditDialog = (AddEditSimpleItemDialog) getFragmentManager().findFragmentByTag(AddEditSimpleItemDialog.FRAGMENT_TAG);
                    String groupName = ((TextView) adapterItemView.findViewById(R.id.text1)).getText().toString();
                    if (mAddEditDialog == null) {
                        mAddEditDialog = AddEditSimpleItemDialog.newInstance("Добавление предмета",
                                "Редактирование предмета",
                                groupName,
                                listItemLocalId, listItemRemoteId);
                    }
                    mAddEditDialog.setDialogListener(mSubjectDialogListener);
                    mAddEditDialog.showInMode(AddEditDialog.MODE_EDIT, getFragmentManager(), AddEditSimpleItemDialog.FRAGMENT_TAG);
                    break;
                case R.id.action_delete:
                    getHostActivity().showProgressBar();
                    mServiceHelperController.deleteSubjects(mToken, new long[]{listItemLocalId}, new long[]{listItemRemoteId}, ApiServiceHelper.PRIORITY_HIGH);
                    break;
                default:
                    return;
            }
        }
    };

    @Override
    protected void restoreCustom() {
        mAddEditDialog = (AddEditSimpleItemDialog) getFragmentManager().findFragmentByTag(AddEditSimpleItemDialog.FRAGMENT_TAG);
        if (mAddEditDialog != null) {
            mAddEditDialog.setDialogListener(mSubjectDialogListener);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri baseUri;
        String[] projection;
        String orderBy;
        switch (id) {
            case LOADER_MAIN:
                baseUri = Contract.Subjects.URI;
                projection = new String[]{Contract.Subjects._ID + " AS _id",
                        Contract.Subjects.Remote.REMOTE_ID,
                        Contract.Subjects.FIELD_NAME,
                        Contract.Subjects.ENTITY_STATUS};
                orderBy = Contract.Subjects.FIELD_NAME + " ASC";
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
    public void onItemClick(View itemView, long localId, long remoteId) {
        //TODO view subject
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
                        "Удаление предметов",
                        "Внимание! Удалятся не только предметы, но и связанные оценки. Продолжить?");
                confirmDialog.setOnConfirmListener(getConfirmDelListener());
                confirmDialog.show(getFragmentManager(), CONFIRM_DIALOG_TAG);
                return true;
        }
        return true;
    }

    @Override
    protected RemoteIdCursorAdapter.OnItemPopupMenuListener getItemPopupMenuListener() {
        return mGroupPopupListener;
    }

    @Override
    protected int getItemPopupMenuRes() {
        return R.menu.item_edit_del_menu;
    }

    @Override
    protected int getFragmentLayoutRes() {
        return R.layout.fragment_subjects;
    }

    @Override
    protected int getListViewId() {
        return R.id.entities_list_view;
    }

    @Override
    protected RemoteIdCursorAdapter getRemoteIdAdapter() {
        return new RemoteIdCursorAdapter(getActivity(),
                R.layout.item_checkable_1,
                mCursor,
                new String[]{Contract.Subjects.FIELD_NAME},
                new int[]{R.id.text1},
                BaseColumns._ID,
                Contract.Subjects.Remote.REMOTE_ID,
                Contract.Subjects.ENTITY_STATUS,
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
                    mAddEditDialog = AddEditSimpleItemDialog.newInstance("Добавить предмет", "Редактировать предмет", "", -1, -1);
                }
                mAddEditDialog.setDialogListener(mSubjectDialogListener);
                mAddEditDialog.showInMode(AddEditDialog.MODE_ADD, getFragmentManager(), AddEditSimpleItemDialog.FRAGMENT_TAG);
                break;
        }
        return true;
    }

    @Override
    protected void notifyAboutRunningActions(int runningCount) {
        if (runningCount > 0) {
            getHostActivity().showProgressBar();
        } else {
            getHostActivity().hideProgressBar();
        }
    }

    @Override
    protected void updateEntitiesList() {
        getHostActivity().showProgressBar();
        mServiceHelperController.getAllSubjects(mToken, ApiServiceHelper.PRIORITY_LOW);
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {
        getHostActivity().hideProgressBar();
        if (actionCode != -1) {

        } else {

        }
    }
}
