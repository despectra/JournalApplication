package com.despectra.android.journal.view.users;

import android.net.Uri;
import android.support.v4.app.Fragment;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.RemoteIdCursorAdapter;
import org.json.JSONException;
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
    protected void performUsersDeletion(long[] localIds, long[] remoteIds) {
        mServiceHelperController.deleteTeachers(mToken, localIds, remoteIds, ApiServiceHelper.PRIORITY_HIGH);
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
        return Uri.parse(Contract.STRING_URI + "/teachers");
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
        return new String[]{Contract.Teachers._ID + " AS _id", Contract.Teachers.Remote.REMOTE_ID,
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
    protected void performOnUserClick(long localId, long remoteId) {
        //TODO
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
    protected RemoteIdCursorAdapter getRemoteIdAdapter() {
        return new RemoteIdCursorAdapter(getActivity(),
                R.layout.item_student_1,
                mCursor,
                new String[]{Contract.Users.FIELD_NAME, Contract.Users.FIELD_SURNAME,
                        Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_LOGIN},
                new int[]{R.id.name_view, R.id.surname_view, R.id.middlename_view, R.id.login_view},
                "_id",
                Contract.Teachers.Remote.REMOTE_ID,
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
    public void onResponse(int actionCode, int remainingActions, Object response) {
        JSONObject jsonResponse = (JSONObject) response;
        if (Utils.isApiJsonSuccess(jsonResponse)) {
            switch (actionCode) {
                case APICodes.ACTION_GET_TEACHERS:
                    getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                    getHostActivity().hideProgressBar();
                    break;
                case APICodes.ACTION_ADD_TEACHER:
                    getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                    getHostActivity().hideProgressBar();
                    break;
                case APICodes.ACTION_DELETE_TEACHERS:
                    getLoaderManager().restartLoader(LOADER_MAIN, null, this);
                    getHostActivity().hideProgressBar();
                    break;
            }
        } else {
            ApiErrorResponder.respondDialog(getChildFragmentManager(), (JSONObject) response);
        }
    }
}
