package com.despectra.android.journal.view.users;

import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.*;
import android.widget.TextView;
import com.despectra.android.journal.view.RemoteIdCursorAdapter;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.view.AddEditDialog;
import com.despectra.android.journal.view.groups.AddEditSimpleItemDialog;
import com.despectra.android.journal.view.SimpleConfirmationDialog;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.view.EntitiesListFragment;

/**
 * Created by Dmitry on 13.04.14.
 */

public class StudentsFragment extends EntitiesListFragment implements ApiServiceHelper.FeedbackApiClient {

    public static final String FRAGMENT_TAG = "studentsFrag";
    public static final String CONFIRM_DIALOG_TAG = "ConfirmDeleteStudents";

    public static final String KEY_LOCAL_GROUP_ID = "localgroupId";
    public static final String KEY_REMOTE_GROUP_ID="remotegroupId";
    public static final String KEY_GROUP_NAME = "groupName";

    private long mLocalGroupId;
    private long mRemoteGroupId;
    private String mGroupName;
    private int mGroupSize;

    private AddEditStudentDialog mAddEditDialog;
    private View mGroupHeaderView;
    private TextView mGroupNameView;
    private TextView mGroupSizeView;

    private AddEditStudentDialog.StudentDialogListener mStudentDialogListener = new AddEditStudentDialog.StudentDialogListener() {
        @Override
        public void onAddStudent(String firstName, String middleName, String secondName, String login) {
            if (mToken.isEmpty()) {
                return;
            }
            getHostActivity().showProgressBar();
            mServiceHelperController.addStudentIntoGroup(mToken, mLocalGroupId, mRemoteGroupId, firstName, middleName, secondName, login, ApiServiceHelper.PRIORITY_HIGH);
        }

        @Override
        public void onEditStudent(long localId, long remoteId, String oldFirstName, String newFirstName, String oldMiddleName, String newMiddleName, String oldSecondName, String newSecondName) {
            //TODO implement updating student info
        }
    };

    private SimpleConfirmationDialog.OnConfirmListener mConfirmDeletingListener = new SimpleConfirmationDialog.OnConfirmListener() {
        @Override
        public void onConfirm() {
            mServiceHelperController.deleteStudents(mToken,
                    mEntitiesAdapter.getCheckedLocalIdsAsArray(),
                    mEntitiesAdapter.getCheckedRemoteIdsAsArray(),
                    ApiServiceHelper.PRIORITY_LOW);
            if (mIsInActionMode) {
                mActionMode.finish();
            }
        }
    };
    private RemoteIdCursorAdapter.OnItemPopupMenuListener mGroupPopupListener = new RemoteIdCursorAdapter.OnItemPopupMenuListener() {
        @Override
        public void onMenuItemSelected(MenuItem item, View adapterItemView, long listItemLocalId, long listItemRemoteId) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    mAddEditDialog = (AddEditStudentDialog) getFragmentManager().findFragmentByTag(AddEditSimpleItemDialog.FRAGMENT_TAG);
                    String name = ((TextView) adapterItemView.findViewById(R.id.name_view)).getText().toString();
                    String surname = ((TextView) adapterItemView.findViewById(R.id.surname_view)).getText().toString();
                    String middlename = ((TextView) adapterItemView.findViewById(R.id.middlename_view)).getText().toString();
                    String login = ((TextView) adapterItemView.findViewById(R.id.login_view)).getText().toString();
                    if (mAddEditDialog == null) {
                        mAddEditDialog = AddEditStudentDialog.newInstance(listItemLocalId, listItemRemoteId,
                                name, middlename, surname, login);
                    }
                    mAddEditDialog.setStudentDialogListener(mStudentDialogListener);
                    mAddEditDialog.showInMode(AddEditDialog.MODE_EDIT, getFragmentManager(), AddEditStudentDialog.FRAGMENT_TAG);
                    break;
                case R.id.action_delete:
                    getHostActivity().showProgressBar();
                    mServiceHelperController.deleteStudents(mToken, new long[]{listItemLocalId}, new long[]{listItemRemoteId}, ApiServiceHelper.PRIORITY_HIGH);
                    break;
                default:
                    return;
            }
        }
    };

    public static StudentsFragment newInstance(String groupName, long localGroupId, long remoteGroupId) {
        StudentsFragment fragment = new StudentsFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_LOCAL_GROUP_ID, localGroupId);
        args.putLong(KEY_REMOTE_GROUP_ID, remoteGroupId);
        args.putString(KEY_GROUP_NAME, groupName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mLocalGroupId = args.getLong(KEY_LOCAL_GROUP_ID);
        mRemoteGroupId = args.getLong(KEY_REMOTE_GROUP_ID);
        mGroupName = args.getString(KEY_GROUP_NAME);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mGroupHeaderView = LayoutInflater.from(getActivity()).inflate(R.layout.group_list_header, null);
        mGroupNameView = (TextView) mGroupHeaderView.findViewById(R.id.group_name);
        mGroupSizeView = (TextView) mGroupHeaderView.findViewById(R.id.group_size);
        mGroupNameView.setText(mGroupName);
        mEntitiesListView.addHeaderView(mGroupHeaderView);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected void restoreCustom() {
        mAddEditDialog = (AddEditStudentDialog) getFragmentManager().findFragmentByTag(AddEditStudentDialog.FRAGMENT_TAG);
        if (mAddEditDialog != null) {
            mAddEditDialog.setStudentDialogListener(mStudentDialogListener);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_students_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_student:
                if (mAddEditDialog == null) {
                    mAddEditDialog = AddEditStudentDialog.newInstance(-1, -1, "", "", "", "");
                }
                mAddEditDialog.setStudentDialogListener(mStudentDialogListener);
                mAddEditDialog.showInMode(AddEditDialog.MODE_ADD, getFragmentManager(), AddEditStudentDialog.TAG);
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
                uri = Uri.parse(String.format("%s/groups_remote/%d/students_remote", Contract.STRING_URI, mLocalGroupId));
                selection = Contract.StudentsGroups.FIELD_GROUP_ID + " = ?";
                selectionArgs = new String[]{String.valueOf(mLocalGroupId)};
                projection = new String[]{Contract.Students._ID + " AS _id", Contract.Students.Remote.REMOTE_ID,
                        Contract.Users.FIELD_NAME, Contract.Users.FIELD_SURNAME,
                        Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_LOGIN,
                        Contract.Users.ENTITY_STATUS};
                orderBy = Contract.Users.FIELD_SURNAME + " ASC";
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

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        super.onLoadFinished(cursorLoader, cursor);
        mGroupSize = cursor.getCount();
        mGroupSizeView.setText(String.format("Учеников: %d", mGroupSize));
    }

    @Override
    public void onItemClick(View itemView, long localId, long remoteId) {
        //TODO student info
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
                        "Удаление учеников",
                        "Внимание! Удалятся не только ученики, но и все связанные уроки, связи с учителями и прочее. Продолжить?");
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
        return R.layout.fragment_simple_entities_list;
    }

    @Override
    protected int getListViewId() {
        return R.id.entities_list_view;
    }

    @Override
    protected RemoteIdCursorAdapter getRemoteIdAdapter() {
        return new RemoteIdCursorAdapter(getActivity(),
                R.layout.item_student_1,
                mCursor,
                new String[]{Contract.Users.FIELD_SURNAME, Contract.Users.FIELD_NAME, Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_LOGIN},
                new int[]{R.id.surname_view, R.id.name_view, R.id.middlename_view, R.id.login_view},
                BaseColumns._ID,
                Contract.Students.Remote.REMOTE_ID,
                Contract.Users.ENTITY_STATUS,
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
        mServiceHelperController.getStudentsByGroup(mToken, mLocalGroupId, mRemoteGroupId, ApiServiceHelper.PRIORITY_LOW);
    }

    @Override
    public void onProgress(Object data) {
        if (data.equals("cached")) {
            getActivity().getSupportLoaderManager().restartLoader(LOADER_MAIN, null, this);
        }
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {
        if (actionCode != -1) {
            switch (actionCode) {
                case APICodes.ACTION_GET_STUDENTS_BY_GROUP:
                    getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                    getHostActivity().hideProgressBar();
                    break;
                case APICodes.ACTION_ADD_STUDENT_IN_GROUP:
                    getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                    getHostActivity().hideProgressBar();
                    break;
                case APICodes.ACTION_DELETE_STUDENTS:
                    getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                    getHostActivity().hideProgressBar();
                    break;
            }
        }
    }
}