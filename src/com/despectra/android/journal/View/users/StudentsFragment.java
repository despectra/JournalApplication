package com.despectra.android.journal.view.users;

import android.net.Uri;
import android.os.Bundle;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.view.MultipleRemoteIdsCursorAdapter;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import org.json.JSONObject;

/**
 * Created by Dmitry on 13.04.14.
 */

public class StudentsFragment extends AbstractUsersFragment {

    public static final String FRAGMENT_TAG = "studentsFrag";
    public static final String CONFIRM_DIALOG_TAG = "ConfirmDeleteStudents";

    public static final String KEY_GROUP_IDS = "groupIds";
    public static final String KEY_GROUP_NAME = "groupName";

    private EntityIds mGroupIds;
    private String mGroupName;

    public static StudentsFragment newInstance(String groupName, EntityIds groupIds) {
        StudentsFragment fragment = new StudentsFragment();
        Bundle args = new Bundle();
        args.putBundle(KEY_GROUP_IDS, groupIds.toBundle());
        args.putString(KEY_GROUP_NAME, groupName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mGroupIds = EntityIds.fromBundle(args.getBundle(KEY_GROUP_IDS));
        mGroupName = args.getString(KEY_GROUP_NAME);
    }

    @Override
    protected void performUserUpdating() {
        //TODO
    }

    @Override
    protected void performUserAddition(String firstName, String middleName, String secondName, String login) {
        mServiceHelperController.addStudentIntoGroup(mToken, mGroupIds,
                firstName, middleName, secondName, login, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    protected void performUsersDeletion(JoinedEntityIds[] ids) {
        EntityIds[] studentsIds = new EntityIds[ids.length];
        for (int i = 0; i < ids.length; i++) {
            studentsIds[i] = ids[i].getIdsByTable(Contract.Students.TABLE);
        }
        mServiceHelperController.deleteStudents(mToken, studentsIds, ApiServiceHelper.PRIORITY_HIGH);
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
        return Uri.parse(String.format("%s/groups/%d/students", Contract.STRING_URI, mGroupIds.getLocalId()));
    }

    @Override
    protected String getLoaderSelection() {
        return Contract.StudentsGroups.FIELD_GROUP_ID + " = ?";
    }

    @Override
    protected String[] getLoaderSelectionArgs() {
        return new String[]{String.valueOf(mGroupIds.getLocalId())};
    }

    @Override
    protected String[] getLoaderProjection() {
        return new String[]{Contract.Students._ID + " AS _id", Contract.Students.REMOTE_ID,
                Contract.Users._ID, Contract.Users.REMOTE_ID,
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
    protected void performOnUserClick(JoinedEntityIds userIds) {
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
        mServiceHelperController.getStudentsByGroup(mToken, mGroupIds, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    public int getActionModeMenuRes() {
        return R.menu.groups_fragment_cab_menu;
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter getRemoteIdAdapter() {
        EntityIdsColumns[] columns = new EntityIdsColumns[] {
            new EntityIdsColumns(Contract.Users.TABLE, Contract.Users._ID, Contract.Users.REMOTE_ID),
            new EntityIdsColumns(Contract.Students.TABLE, "_id", Contract.Students.REMOTE_ID)
        };
        return new MultipleRemoteIdsCursorAdapter(getActivity(),
                R.layout.item_student_1,
                mCursor,
                new String[]{Contract.Users.FIELD_SURNAME, Contract.Users.FIELD_NAME, Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_LOGIN},
                new int[]{R.id.surname_view, R.id.name_view, R.id.middlename_view, R.id.login_view},
                columns,
                Contract.Users.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.item_popup_menu_btn1,
                0);
    }

    @Override
    protected String getConfirmDelDialogTag() {
        return CONFIRM_DIALOG_TAG;
    }

    @Override
    protected String getTitle() {
        return "Обзор класса";
    }

    @Override
    protected String getEmptyListMessage() {
        return "Учеников в этом классе нет. Добавьте с помощью кнопки на панели действий";
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        /*switch (actionCode) {*/
        hideProgress();
            /*case APICodes.ACTION_GET_STUDENTS_BY_GROUP:
                getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                hideProgress();
                break;
            case APICodes.ACTION_ADD_STUDENT_IN_GROUP:
                getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                hideProgress();
                break;
            case APICodes.ACTION_DELETE_STUDENTS:
                getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                hideProgress();
                break;*/
        /*}*/
    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        ApiErrorResponder.respondDialog(getFragmentManager(), (JSONObject) response);
    }
}
