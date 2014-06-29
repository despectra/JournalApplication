package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.content.Entity;
import android.database.Cursor;
import android.os.SystemClock;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.local.TableModel;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import com.despectra.android.journal.utils.JSONBuilder;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Dmitry on 02.06.14.
 */
public class Teachers extends QueryExecDelegate {

    private Contract.EntityTable mTeachersTable;
    private Contract.EntityTable mTSTable;

    public Teachers(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
        mTeachersTable = TableModel.getTable(Contract.Teachers.TABLE);
        mTSTable = TableModel.getTable(Contract.TeachersSubjects.TABLE);
    }

    //####################   RETRIEVING   ########################

    public JSONObject get(ApiAction action) throws Exception {
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalTeachers(response);
            getLocalStorageManager().notifyUri(Contract.Teachers.URI_AS_USERS);
        }
        return response;
    }

    private void updateLocalTeachers(JSONObject response) throws Exception {
        Cursor localTeachers = getLocalStorageManager().getResolver().query(
                Contract.Teachers.URI,
                new String[]{Contract.Teachers.REMOTE_ID, Contract.Teachers._ID},
                null,
                null,
                null
        );
        JSONArray remoteTeachers = response.getJSONArray("teachers");
        for (int i = 0; i < remoteTeachers.length(); i++) {
            JSONObject teacher = remoteTeachers.getJSONObject(i);
            teacher.put("level", 4);
        }
        getLocalStorageManager().updateComplexEntityWithJsonResponse(LocalStorageManager.MODE_REPLACE,
                localTeachers,
                mTeachersTable,
                remoteTeachers,
                null
        );
    }

    public JSONObject getOne(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherId = request.getLong("LOCAL_teacher_id");
        request.remove("user_id");
        request.remove("LOCAL_teacher_id");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            //update user data
            response.put("user_id", getLocalStorageManager().getLocalIdByRemote(Contract.Users.HOLDER, response.getLong("user_id")));
            getLocalStorageManager().commitUpdatingEntity(mTeachersTable, localTeacherId, response);;
        } else {
            int errorCode = response.getInt("error_code");
            switch (errorCode) {
                case 100:
                    //delete non-existing record
                    getLocalStorageManager().commitDeletingEntitiesCascade(mTeachersTable, new long[]{localTeacherId});
                    break;
                default:
                    break;
            }
        }

        return response;
    }

    //##################   ADDITION   ###########################

    public JSONObject add(ApiAction action) throws Exception {
        JSONObject request = action.actionData;

        request.put("level", 4);
        long localTeacherId = getLocalStorageManager().preInsertEntity(mTeachersTable, request);
        //notify UI loaders
        getLocalStorageManager().notifyUri(Contract.Teachers.URI_AS_USERS);

        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            //commit
            getLocalStorageManager().commitInsertingEntity(mTeachersTable, localTeacherId, response);
        } else {
            //rollback
            getLocalStorageManager().rollbackInsertingEntity(mTeachersTable, localTeacherId);
        }
        getLocalStorageManager().notifyUri(Contract.Teachers.URI_AS_USERS);
        return response;
    }

    //#############    DELETION    ###################

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long[] localTeachersIds = Utils.getIdsFromJSONArray(request.getJSONArray("LOCAL_teachers"));
        getLocalStorageManager().preDeleteEntitiesCascade(mTeachersTable, localTeachersIds);

        getLocalStorageManager().notifyUri(Contract.Teachers.URI_AS_USERS);
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            //commit
            getLocalStorageManager().commitDeletingEntitiesCascade(mTeachersTable, localTeachersIds);
        } else {
            //rollback
            getLocalStorageManager().rollbackDeletingEntityCascade(mTeachersTable, localTeachersIds);
        }
        getLocalStorageManager().notifyUri(Contract.Teachers.URI_AS_USERS);
        return response;
    }

    /// $#######  SUBJECTS OF TEACHER SECTION
    // ###########    RETRIEVING   ###############

    public JSONObject getSubjectsOfAllTeachers(ApiAction action) throws Exception {
        return getSubjectsOfTeacherImpl(action, true);
    }

    public JSONObject getSubjectsOfTeacher(ApiAction action) throws Exception {
        return getSubjectsOfTeacherImpl(action, false);
    }

    private JSONObject getSubjectsOfTeacherImpl(ApiAction action, boolean forAllTeachers) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherId = 0;
        if (!forAllTeachers) {
            localTeacherId = request.getLong("LOCAL_teacher_id");
            request.remove("LOCAL_teacher_id");
        }
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalSubjectsLinks(forAllTeachers, localTeacherId, response);
            if (!forAllTeachers) {
                getLocalStorageManager().notifyUri(Contract.TeachersSubjects.URI_WITH_SUBJECTS);
            }
        }
        return response;
    }

    private void updateLocalSubjectsLinks(boolean forAllTeachers, long teacherId, JSONObject response) throws Exception {
        String selection = forAllTeachers ? null : Contract.TeachersSubjects.FIELD_TEACHER_ID + " = ?";
        String[] selectionArgs = forAllTeachers ? null : new String[]{String.valueOf(teacherId)};
        Cursor existingSubjects = getLocalStorageManager().getResolver().query(
                Contract.TeachersSubjects.URI,
                new String[]{Contract.TeachersSubjects._ID, Contract.TeachersSubjects.REMOTE_ID},
                selection,
                selectionArgs,
                null
        );
        JSONArray remoteSubjects = response.getJSONArray("subjects");
        for (int i = 0; i < remoteSubjects.length(); i++) {
            JSONObject item = remoteSubjects.getJSONObject(i);
            item.put("teacher_id", forAllTeachers
                    ? getLocalStorageManager().getLocalIdByRemote(
                    Contract.Teachers.HOLDER,
                    item.getLong("teacher_id"))
                    : teacherId);
            item.put("subject_id",
                    getLocalStorageManager().getLocalIdByRemote(
                            Contract.Subjects.HOLDER,
                            item.getLong("subject_id")));
        }

        getLocalStorageManager().updateComplexEntityWithJsonResponse(LocalStorageManager.MODE_REPLACE,
                existingSubjects,
                mTSTable,
                remoteSubjects,
                null
        );
    }

    // ######  SETTING  #########

    public JSONObject setSubjectsOfTeacher(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherId = request.getLong("LOCAL_teacher_id");
        long[] localSubjectsIds = Utils.getIdsFromJSONArray(request.getJSONArray("LOCAL_subjects_ids"));
        request.remove("LOCAL_teacher_id");
        request.remove("LOCAL_subjects_ids");

        long[] localLinksIds = preSetLocalSubjectsOfTeacher(localTeacherId, localSubjectsIds);
        getLocalStorageManager().notifyUri(Contract.TeachersSubjects.URI_WITH_SUBJECTS);
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            commitSetSubjectsOfTeacher(localLinksIds, Utils.getIdsFromJSONArray(response.getJSONArray("affected_links")));
        } else {
            rollbackSetSubjectsOfTeacher(localLinksIds);
        }
        getLocalStorageManager().notifyUri(Contract.TeachersSubjects.URI_WITH_SUBJECTS);
        return response;
    }

    private long[] preSetLocalSubjectsOfTeacher(long localTeacherId, long[] localSubjectsIds) throws Exception {
        long[] tempIds = new long[localSubjectsIds.length];
        for (int i = 0; i < localSubjectsIds.length; i++) {
            JSONObject insertingRow = new JSONBuilder()
                    .addKeyValue("teacher_id", localTeacherId)
                    .addKeyValue("subject_id", localSubjectsIds[i]).create();
            tempIds[i] = getLocalStorageManager().preInsertEntity(mTSTable, insertingRow);
        }
        return tempIds;
    }

    private void commitSetSubjectsOfTeacher(long[] localTempIds, long[] affectedIds) throws Exception {
        for (int i = 0; i < affectedIds.length; i++) {
            JSONObject idRow = new JSONBuilder()
                    .addKeyValue("teacher_subject_id", affectedIds[i]).create();
            getLocalStorageManager().commitInsertingEntity(mTSTable, localTempIds[i], idRow);
        }
    }

    private void rollbackSetSubjectsOfTeacher(long[] localLinksIds) throws Exception {
        for (int i = 0; i < localLinksIds.length; i++) {
            getLocalStorageManager().rollbackInsertingEntity(mTSTable, localLinksIds[i]);
        }
    }

    //######   UNSETTING   #######

    public JSONObject unsetSubjectsOfTeacher(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long[] localLinks = Utils.getIdsFromJSONArray(request.getJSONArray("LOCAL_links_ids"));
        request.remove("LOCAL_links_ids");

        preUnsetSubjectsOfTeacher(localLinks);
        getLocalStorageManager().notifyUri(Contract.TeachersSubjects.URI_WITH_SUBJECTS);
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            commitUnsetSubjectsOfTeacher(localLinks);
        } else {
            rollbackUnsetSubjectsOfTeacher(localLinks);
        }
        getLocalStorageManager().notifyUri(Contract.TeachersSubjects.URI_WITH_SUBJECTS);
        return response;
    }

    private void preUnsetSubjectsOfTeacher(long[] localLinksIds) throws Exception {
        getLocalStorageManager().preDeleteEntitiesCascade(mTSTable, localLinksIds);
    }

    private void commitUnsetSubjectsOfTeacher(long[] localLinksIds) throws Exception {
        getLocalStorageManager().commitDeletingEntitiesCascade(mTSTable, localLinksIds);
    }

    private void rollbackUnsetSubjectsOfTeacher(long[] localLinksIds) throws Exception {
        getLocalStorageManager().rollbackDeletingEntityCascade(mTSTable, localLinksIds);
    }
}