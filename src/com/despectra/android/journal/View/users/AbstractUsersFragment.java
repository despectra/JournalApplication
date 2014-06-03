package com.despectra.android.journal.view.users;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.view.*;

/**
 * Created by Dmitry on 13.04.14.
 */

public abstract class AbstractUsersFragment extends EntitiesListFragment implements ApiServiceHelper.FeedbackApiClient {

    private AddEditSimpleUserDialog mAddEditDialog;
    private View mHeaderView;
    private RelativeLayout mHeaderLayout;
    private TextView mUsersTitleView;
    private TextView mUsersCountView;

    private AddEditSimpleUserDialog.DialogListener mUserDialogListener = new AddEditSimpleUserDialog.DialogListener() {
        @Override
        public void onAddUser(String firstName, String middleName, String secondName, String login) {
            if (mToken.isEmpty()) {
                return;
            }
            showProgress();
            performUserAddition(firstName, middleName, secondName, login);
        }

        @Override
        public void onEditUser(JoinedEntityIds ids, String oldFirstName, String newFirstName, String oldMiddleName, String newMiddleName, String oldSecondName, String newSecondName) {
            performUserUpdating();
        }
    };

    protected abstract void performUserUpdating();

    protected abstract void performUserAddition(String firstName, String middleName, String secondName, String login);

    private SimpleConfirmationDialog.OnConfirmListener mConfirmDeletingListener = new SimpleConfirmationDialog.OnConfirmListener() {
        @Override
        public void onConfirm() {
            performUsersDeletion(mEntitiesAdapter.getCheckedIds());
            if (mIsInActionMode) {
                mActionMode.finish();
            }
        }
    };

    protected abstract void performUsersDeletion(JoinedEntityIds[] ids);

    private MultipleRemoteIdsCursorAdapter.OnItemPopupMenuListener mGroupPopupListener = new MultipleRemoteIdsCursorAdapter.OnItemPopupMenuListener() {
        @Override
        public void onMenuItemSelected(MenuItem item, View adapterItemView, JoinedEntityIds ids) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    mAddEditDialog = (AddEditSimpleUserDialog) getFragmentManager().findFragmentByTag(AddEditSimpleItemDialog.FRAGMENT_TAG);
                    String name = ((TextView) adapterItemView.findViewById(R.id.name_view)).getText().toString();
                    String surname = ((TextView) adapterItemView.findViewById(R.id.surname_view)).getText().toString();
                    String middlename = ((TextView) adapterItemView.findViewById(R.id.middlename_view)).getText().toString();
                    String login = ((TextView) adapterItemView.findViewById(R.id.login_view)).getText().toString();
                    if (mAddEditDialog == null) {
                        mAddEditDialog = AddEditSimpleUserDialog.newInstance(ids,
                                getAddEditDialogAddTitle(), getAddEditDialogEditTitle(),
                                name, middlename, surname, login);
                    }
                    mAddEditDialog.setDialogListener(mUserDialogListener);
                    mAddEditDialog.showInMode(AddEditDialog.MODE_EDIT, getFragmentManager(), AddEditSimpleUserDialog.FRAGMENT_TAG);
                    break;
                case R.id.action_delete:
                    showProgress();
                    performUsersDeletion(new JoinedEntityIds[]{ids});
                    break;
                default:
                    return;
            }
        }
    };

    protected abstract String getAddEditDialogEditTitle();

    protected abstract String getAddEditDialogAddTitle();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mHeaderView = LayoutInflater.from(getActivity()).inflate(R.layout.entities_list_header, null);
        mHeaderLayout = (RelativeLayout) mHeaderView.findViewById(R.id.entities_list_header_layout);
        mUsersTitleView = (TextView) mHeaderView.findViewById(R.id.entities_title);
        mUsersCountView = (TextView) mHeaderView.findViewById(R.id.entities_count);
        mUsersTitleView.setText(getUsersTitle());
        mHeaderLayout.setBackgroundResource(getHeaderBackgroundRes());
        mEntitiesListView.addHeaderView(mHeaderView);
        super.onActivityCreated(savedInstanceState);
    }

    protected abstract int getHeaderBackgroundRes();

    protected abstract String getUsersTitle();

    @Override
    protected void restoreCustom() {
        mAddEditDialog = (AddEditSimpleUserDialog) getFragmentManager().findFragmentByTag(AddEditSimpleUserDialog.FRAGMENT_TAG);
        if (mAddEditDialog != null) {
            mAddEditDialog.setDialogListener(mUserDialogListener);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(getOptionsMenuRes(), menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    protected abstract int getOptionsMenuRes();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (mAddEditDialog == null) {
                    mAddEditDialog = AddEditSimpleUserDialog.newInstance(new JoinedEntityIds(), getAddEditDialogAddTitle(), getAddEditDialogEditTitle(),
                            "", "", "", "");
                }
                mAddEditDialog.setDialogListener(mUserDialogListener);
                mAddEditDialog.showInMode(AddEditDialog.MODE_ADD, getFragmentManager(), AddEditSimpleUserDialog.TAG);
                break;
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri uri;
        String selection;
        String[] projection;
        String[] selectionArgs;
        String orderBy;
        switch (id) {
            case LOADER_MAIN:
                uri = getLoaderUri();
                selection = getLoaderSelection();
                selectionArgs = getLoaderSelectionArgs();
                projection = getLoaderProjection();
                orderBy = getLoaderOrderBy();
                break;
            default:
                return null;
        }
        return new CursorLoader(
                getActivity(),
                uri,
                projection,
                selection,
                selectionArgs,
                orderBy
        );
    }

    protected abstract Uri getLoaderUri();

    protected abstract String getLoaderSelection();

    protected abstract String[] getLoaderSelectionArgs();

    protected abstract String[] getLoaderProjection();

    protected abstract String getLoaderOrderBy();

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        super.onLoadFinished(cursorLoader, cursor);
        mUsersCountView.setText(String.format("%s: %d", getUsersCountStringBeginning(), cursor.getCount()));
    }

    protected abstract String getUsersCountStringBeginning();

    @Override
    public void onItemClick(View itemView, JoinedEntityIds ids) {
        performOnUserClick(ids);
    }

    protected abstract void performOnUserClick(JoinedEntityIds ids);

    @Override
    public boolean onActionModeItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                SimpleConfirmationDialog confirmDialog = SimpleConfirmationDialog.newInstance(
                        getConfirmDelDialogTitle(),
                        getConfirmDelDialogMessage());
                confirmDialog.setOnConfirmListener(getConfirmDelListener());
                confirmDialog.show(getFragmentManager(), getConfirmDelDialogTag());
                return true;
        }
        return true;
    }

    protected abstract String getConfirmDelDialogTitle();
    protected abstract String getConfirmDelDialogMessage();

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
    protected SimpleConfirmationDialog.OnConfirmListener getConfirmDelListener() {
        return mConfirmDeletingListener;
    }

    @Override
    protected void notifyAboutRunningActions(int runningCount) {
        if (runningCount > 0) {
            showProgress();
        } else {
            hideProgress();
        }
    }

    @Override
    protected void updateEntitiesList() {
        showProgress();
        performUpdatingUsersList();
    }

    protected abstract void performUpdatingUsersList();

    @Override
    public void onProgress(Object data) {
        if (data.equals("cached")) {
            getActivity().getSupportLoaderManager().restartLoader(LOADER_MAIN, null, this);
        }
    }
}