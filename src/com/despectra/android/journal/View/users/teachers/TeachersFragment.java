package com.despectra.android.journal.view.users.teachers;

import android.content.Intent;
import android.net.Uri;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.view.MultipleRemoteIdsCursorAdapter;
import com.despectra.android.journal.view.users.AbstractUsersFragment;
import org.json.JSONObject;

/**
 * Created by Dmitry on 30.05.14.
 */
public class TeachersFragment extends AbstractUsersFragment {
    @Override
    protected void performUserUpdating() {
        //TODO
    }

    @Override
    protected void performUserAddition(String firstName, String middleName, String secondName, String login) {
        mServiceHelperController.addTeacher(mToken, firstName, middleName, secondName, login, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    protected void performUsersDeletion(JoinedEntityIds[] ids) {
        EntityIds[] teachersIds = new EntityIds[ids.length];
        for (int i = 0; i < ids.length; i++) {
            teachersIds[i] = ids[i].getIdsByTable(Contract.Teachers.TABLE);
        }
        mServiceHelperController.deleteTeachers(mToken, teachersIds, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    protected String getAddEditDialogEditTitle() {
        return "Редактировать учителя";
    }

    @Override
    protected String getAddEditDialogAddTitle() {
        return "Добавить учителя";
    }

    @Override
    protected int getHeaderBackgroundRes() {
        return R.drawable.teachers_header_bg;
    }

    @Override
    protected String getUsersTitle() {
        return "Учителя";
    }

    @Override
    protected int getOptionsMenuRes() {
        return R.menu.fragment_teacher_menu;
    }

    @Override
    protected Uri getLoaderUri() {
        return Contract.Teachers.URI_AS_USERS;
    }

    @Override
    protected String getLoaderSelection() {
        return null;
    }

    @Override
    protected String[] getLoaderSelectionArgs() {
        return null;
    }

    @Override
    protected String[] getLoaderProjection() {
        return new String[]{Contract.Teachers._ID + " AS _id", Contract.Teachers.REMOTE_ID,
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
        return "Всего";
    }

    @Override
    protected void performOnUserClick(JoinedEntityIds ids) {
        //TODO
        Intent intent = new Intent(getActivity(), TeacherActivity.class);
        intent.putExtra("userId", ids.toBundle());
        startActivity(intent);
    }

    @Override
    protected String getConfirmDelDialogTitle() {
        return "Удаление учителей";
    }

    @Override
    protected String getConfirmDelDialogMessage() {
        return "Удалятся не только записи об учителях, но и связи с предметами и классами. Продолжить?";
    }

    @Override
    protected void performUpdatingUsersList() {
        mServiceHelperController.getTeachers(mToken, 0, 0, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    public int getActionModeMenuRes() {
        return R.menu.groups_fragment_cab_menu;
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter getRemoteIdAdapter() {
        EntityIdsColumns[] idsColumns = new EntityIdsColumns[]{
                new EntityIdsColumns(Contract.Users.TABLE, Contract.Users._ID, Contract.Users.REMOTE_ID),
                new EntityIdsColumns(Contract.Teachers.TABLE, "_id", Contract.Teachers.REMOTE_ID)
        };

        return new MultipleRemoteIdsCursorAdapter(getActivity(),
                R.layout.item_student_1,
                mCursor,
                new String[]{Contract.Users.FIELD_NAME, Contract.Users.FIELD_SURNAME,
                        Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_LOGIN},
                new int[]{R.id.name_view, R.id.surname_view, R.id.middlename_view, R.id.login_view},
                idsColumns,
                Contract.Users.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.dropdown_btn1,
                0);
    }

    @Override
    protected String getConfirmDelDialogTag() {
        return "ConfirmDeleteTeachers";
    }

    @Override
    protected String getTitle() {
        return null;
    }

    @Override
    protected String getEmptyListMessage() {
        return "Учителей нет. Добавьте их с помощью кнопки на панели действий";
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        hideProgress();
        /*switch (actionCode) {
            case APICodes.ACTION_GET_TEACHERS:
                getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                hideProgress();
                break;
            case APICodes.ACTION_ADD_TEACHER:
                getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                hideProgress();
                break;
            case APICodes.ACTION_DELETE_TEACHERS:
                getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                hideProgress();
                break;
        }*/
    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        ApiErrorResponder.respondDialog(getChildFragmentManager(), (JSONObject) response);
    }
}
