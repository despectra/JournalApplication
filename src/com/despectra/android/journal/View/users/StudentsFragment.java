package com.despectra.android.journal.view.users;

import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.*;
import android.widget.TextView;
import com.despectra.android.journal.logic.services.ApiService;
import com.despectra.android.journal.view.RemoteIdCursorAdapter;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.view.AddEditDialog;
import com.despectra.android.journal.view.AddEditSimpleItemDialog;
import com.despectra.android.journal.view.SimpleConfirmationDialog;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.view.EntitiesListFragment;

/**
 * Created by Dmitry on 13.04.14.
 */

public class StudentsFragment extends AbstractUsersFragment {

    public static final String FRAGMENT_TAG = "studentsFrag";
    public static final String CONFIRM_DIALOG_TAG = "ConfirmDeleteStudents";

    public static final String KEY_LOCAL_GROUP_ID = "localgroupId";
    public static final String KEY_REMOTE_GROUP_ID="remotegroupId";
    public static final String KEY_GROUP_NAME = "groupName";

    private long mLocalGroupId;
    private long mRemoteGroupId;
    private String mGroupName;

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
    protected void performUserUpdating() {
        //TODO
    }

    @Override
    protected void performUserAddition(String firstName, String middleName, String secondName, String login) {
        mServiceHelperController.addStudentIntoGroup(mToken, mLocalGroupId, mRemoteGroupId,
                firstName, middleName, secondName, login, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    protected void performUsersDeletion(long[] localIds, long remoteIds[]) {
        mServiceHelperController.deleteStudents(mToken, localIds, remoteIds, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    protected String getAddEditDialogEditTitle() {
        return "Редактирование ученика";
    }

    @Override
    protected String getAddEditDialogAddTitle() {
        return "Добавление ученика";
    }

    @Override
    protected int getHeaderBackgroundRes() {
        return R.drawable.group_header_bg;
    }

    @Override
    protected String getUsersTitle() {
        return mGroupName;
    }

    @Override
    protected int getOptionsMenuRes() {
        return R.menu.fragment_students_menu;
    }

    @Override
    protected Uri getLoaderUri() {
        return Uri.parse(String.format("%s/groups_remote/%d/students_remote", Contract.STRING_URI, mLocalGroupId));
    }

    @Override
    protected String getLoaderSelection() {
        return Contract.StudentsGroups.FIELD_GROUP_ID + " = ?";
    }

    @Override
    protected String[] getLoaderSelectionArgs() {
        return new String[]{String.valueOf(mLocalGroupId)};
    }

    @Override
    protected String[] getLoaderProjection() {
        return new String[]{Contract.Students._ID + " AS _id", Contract.Students.Remote.REMOTE_ID,
                Contract.Users.FIELD_NAME, Contract.Users.FIELD_SURNAME,
                Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_LOGIN,
                Contract.Users.ENTITY_STATUS};
    }

    @Override
    protected String getLoaderOrderBy() {
        return Contract.Users.FIELD_SURNAME + " ASC";
    }

    @Override
    protected String getUsersCountStringBeginning() {
        return "Учеников";
    }

    @Override
    protected void performOnUserClick(long localId, long remoteId) {
        //TODO
    }

    @Override
    protected String getConfirmDelDialogTitle() {
        return "Уделание учеников";
    }

    @Override
    protected String getConfirmDelDialogMessage() {
        return "Внимание! Удалятся не только ученики, но и все связанные уроки, связи с учителями и прочее. Продолжить?";
    }

    @Override
    protected void performUpdatingUsersList() {
        mServiceHelperController.getStudentsByGroup(mToken, mLocalGroupId, mRemoteGroupId, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    public int getActionModeMenuRes() {
        return R.menu.groups_fragment_cab_menu;
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
    protected String getConfirmDelDialogTag() {
        return CONFIRM_DIALOG_TAG;
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
