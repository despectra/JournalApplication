package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
* Created by Dmitry on 02.06.14.
*/
public class Teachers extends QueryExecDelegate {
    public Teachers(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
    }

    //####################   RETRIEVING   ########################

    public JSONObject get(ApiAction action) throws Exception {
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalTeachers(response);
            getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");
        }
        return response;
    }

    public JSONObject getOne(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherId = request.getLong("LOCAL_teacher_id");
        long localUserId = request.getLong("user_id");
        request.remove("user_id");
        request.remove("LOCAL_teacher_id");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            //update user data
            getLocalStorageManager().updateSingleEntity(
                    Contract.Users.HOLDER,
                    localUserId,
                    response,
                    new String[]{"name", "middlename", "surname", "login", "level"},
                    new String[]{Contract.Users.FIELD_NAME, Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_SURNAME,
                        Contract.Users.FIELD_LOGIN, Contract.Users.FIELD_LEVEL}
            );
        } else {
            int errorCode = response.getInt("error_code");
            switch (errorCode) {
                case 100:
                    //delete non-existing record
                    deleteLocalTeacher(localUserId, localTeacherId);
                    break;
                default:
                    break;
            }
        }

        return response;
    }

    private void updateLocalTeachers(JSONObject response) throws JSONException {
        Cursor localTeachers = getLocalStorageManager().getResolver().query(
                Contract.Teachers.URI_AS_USERS,
                new String[]{Contract.Teachers.REMOTE_ID, Contract.Teachers._ID,
                        Contract.Users.REMOTE_ID, Contract.Users._ID},
                null,
                null,
                null
        );
        JSONArray remoteTeachers = response.getJSONArray("teachers");
        for (int i = 0; i < remoteTeachers.length(); i++) {
            JSONObject teacher = remoteTeachers.getJSONObject(i);
            teacher.put("level", 4);
        }
        Map<Long, Long> affectedUsers = getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localTeachers,
                Contract.Users.HOLDER,
                remoteTeachers,
                "user_id",
                new String[]{"name", "middlename", "surname", "login", "level"},
                new String[]{Contract.Users.FIELD_NAME, Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_SURNAME,
                        Contract.Users.FIELD_LOGIN, Contract.Users.FIELD_LEVEL}
        );
        for (int i = 0; i < remoteTeachers.length(); i++) {
            JSONObject teacher = remoteTeachers.getJSONObject(i);
            teacher.put("user_id", affectedUsers.get(teacher.getLong("user_id")));
        }
        getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localTeachers,
                Contract.Teachers.HOLDER,
                remoteTeachers,
                "teacher_id",
                new String[]{"user_id"},
                new String[]{Contract.Teachers.FIELD_USER_ID}
        );
    }

    //##################   ADDITION   ###########################


    public JSONObject add(ApiAction action) throws Exception {
        JSONObject request = action.actionData;

        long localUserId = Users.preAddUser(this, request, 4);
        long localTeacherId = preAddTeacher(localUserId);
        //notify UI loaders
        getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            //commit
            commitAddTeacher(localUserId, localTeacherId, response);
        } else {
            //rollback
            rollbackAddTeacher(localUserId, localTeacherId);
        }
        getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");
        return response;
    }

    private long preAddTeacher(long localUserId) {
        ContentValues tempTeacher = new ContentValues();
        tempTeacher.put(Contract.Teachers.FIELD_USER_ID, localUserId);
        return getLocalStorageManager().insertTempEntity(Contract.Teachers.HOLDER, tempTeacher);
    }

    private void commitAddTeacher(long localUserId, long localTeacherId, JSONObject response) throws JSONException {
        long remoteUserId = response.getLong("user_id");
        long remoteTeacherId = response.getLong("teacher_id");
        Users.commitAddUser(this, localUserId, remoteUserId);
        getLocalStorageManager().persistTempEntity(Contract.Teachers.HOLDER, localTeacherId, remoteTeacherId);
    }

    private void rollbackAddTeacher(long localUserId, long localTeacherId) {
        Users.rollbackAddUser(this, localUserId);
        getLocalStorageManager().deleteEntityByLocalId(Contract.Teachers.HOLDER, localTeacherId);
    }

    //#############    DELETION    ###################

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONArray teachersIds = request.getJSONArray("LOCAL_teachers");
        long[] usersIds = preDeleteTeachers(request);
        getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            //commit
            commitTeachersDeletion(usersIds, teachersIds);
        } else {
            //rollback
            rollbackTeachersDeletion(usersIds, teachersIds);
        }
        getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");
        return response;
    }

    private long[] preDeleteTeachers(JSONObject request) throws JSONException {
        JSONArray localTeachers = request.getJSONArray("LOCAL_teachers");
        request.remove("LOCAL_teachers");
        long[] localUsersIds = new long[localTeachers.length()];
        for (int i = 0; i < localTeachers.length(); i++) {
            long localTeacherId = localTeachers.getLong(i);
            Cursor localUser = getContext().getContentResolver().query(Contract.Teachers.URI_AS_USERS,
                    new String[]{Contract.Users._ID},
                    Contract.Teachers._ID + " = ?",
                    new String[]{ String.valueOf(localTeacherId) },
                    null);
            localUser.moveToFirst();
            long localUserId = localUser.getLong(0);
            Users.preDeleteUser(this, localUserId);
            getLocalStorageManager().markEntityAsDeleting(Contract.Teachers.HOLDER, localTeacherId);
            localUsersIds[i] = localUserId;
        }
        return localUsersIds;
    }

    private void commitTeachersDeletion(long[] usersIds, JSONArray teachersIds) throws Exception {
        getLocalStorageManager().deleteEntitiesByLocalIds(Contract.Teachers.HOLDER, teachersIds);
        getLocalStorageManager().deleteEntitiesByLocalIds(Contract.Users.HOLDER, usersIds);
    }

    private void rollbackTeachersDeletion(long[] usersIds, JSONArray teachersIds) throws Exception {
        getLocalStorageManager().markEntitiesAsIdle(Contract.Teachers.HOLDER, teachersIds);
        getLocalStorageManager().markEntitiesAsIdle(Contract.Users.HOLDER, usersIds);
    }

    private void deleteLocalTeacher(long localUserId, long localTeacherId) {
        getLocalStorageManager().deleteEntityByLocalId(Contract.Teachers.HOLDER, localTeacherId);
        getLocalStorageManager().deleteEntityByLocalId(Contract.Users.HOLDER, localUserId);
    }

    /// $#######  SUBJECTS OF TEACHER SECTION
    // ###########    RETRIEVING   ###############

    public JSONObject getSubjectsOfTeacher(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherId = request.getLong("LOCAL_teacher_id");
        request.remove("LOCAL_teacher_id");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalSubjectsLinks(localTeacherId, response);
            getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                    action,
                    "SubjectsOfTeacherFragment");
        }
        return response;
    }

    private void updateLocalSubjectsLinks(long teacherId, JSONObject response) throws Exception {
        Cursor existingSubjects = getLocalStorageManager().getResolver().query(
                Contract.TeachersSubjects.URI,
                new String[]{Contract.TeachersSubjects._ID, Contract.TeachersSubjects.REMOTE_ID},
                Contract.TeachersSubjects.FIELD_TEACHER_ID + " = ?",
                new String[]{String.valueOf(teacherId)},
                null
        );
        JSONArray subjects = response.getJSONArray("subjects");
        for (int i = 0; i < subjects.length(); i++) {
            JSONObject subj = subjects.getJSONObject(i);
            subj.put("teacher_id", teacherId);
            long remoteSubjectId = subj.getLong("subject_id");
            long localSubjectId = getLocalStorageManager().getLocalIdByRemote(Contract.Subjects.HOLDER, remoteSubjectId);
            subj.put("LOCAL_subject_id", localSubjectId);
        }
        getLocalStorageManager().updateEntityWithJSONArray(LocalStorageManager.MODE_REPLACE,
                existingSubjects,
                Contract.TeachersSubjects.HOLDER,
                subjects,
                "id",
                new String[]{"teacher_id", "LOCAL_subject_id"},
                new String[]{Contract.TeachersSubjects.FIELD_TEACHER_ID, Contract.TeachersSubjects.FIELD_SUBJECT_ID});
    }

    // ######  SETTING  #########

    public JSONObject setSubjectsOfTeacher(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherId = request.getLong("LOCAL_teacher_id");
        JSONArray localSubjectsIds = request.getJSONArray("LOCAL_subjects_ids");
        request.remove("LOCAL_teacher_id");
        request.remove("LOCAL_subjects_ids");

        long[] localLinksIds = preSetLocalSubjectsOfTeacher(localTeacherId, localSubjectsIds);
        getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                action,
                "SubjectsOfTeacherFragment");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            commitSetSubjectsOfTeacher(localLinksIds, response.getJSONArray("affected_links"));
        } else {
            rollbackSetSubjectsOfTeacher(localLinksIds);
        }
        getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                action,
                "SubjectsOfTeacherFragment");
        return response;
    }

    private long[] preSetLocalSubjectsOfTeacher(long localTeacherId, JSONArray localSubjectsIds) throws Exception {
        long[] tempIds = new long[localSubjectsIds.length()];
        for (int i = 0; i < localSubjectsIds.length(); i++) {
            ContentValues values = new ContentValues();
            values.put(Contract.TeachersSubjects.FIELD_TEACHER_ID, localTeacherId);
            values.put(Contract.TeachersSubjects.FIELD_SUBJECT_ID, localSubjectsIds.getString(i));
            tempIds[i] = getLocalStorageManager().insertTempEntity(Contract.TeachersSubjects.HOLDER, values);
        }
        return tempIds;
    }

    private void commitSetSubjectsOfTeacher(long[] localTempIds, JSONArray affectedIds) throws Exception {
        getLocalStorageManager().persistTempEntities(Contract.TeachersSubjects.HOLDER, localTempIds, affectedIds);
    }

    private void rollbackSetSubjectsOfTeacher(long[] localLinksIds) {
        getLocalStorageManager().deleteEntitiesByLocalIds(Contract.TeachersSubjects.HOLDER, localLinksIds);
    }

    //######   UNSETTING   #######

    public JSONObject unsetSubjectsOfTeacher(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONArray localLinks = request.getJSONArray("LOCAL_links_ids");
        request.remove("LOCAL_links_ids");

        preUnsetSubjectsOfTeacher(localLinks);
        getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                action,
                "SubjectsOfTeacherFragment");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            commitUnsetSubjectsOfTeacher(localLinks);
        } else {
            rollbackUnsetSubjectsOfTeacher(localLinks);
        }
        getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                action,
                "SubjectsOfTeacherFragment");
        return response;
    }

    private void preUnsetSubjectsOfTeacher(JSONArray localLinks) throws Exception {
        getLocalStorageManager().markEntitiesAsDeleting(Contract.TeachersSubjects.HOLDER, localLinks);
    }

    private void commitUnsetSubjectsOfTeacher(JSONArray localLinks) throws Exception {
        getLocalStorageManager().deleteEntitiesByLocalIds(Contract.TeachersSubjects.HOLDER, localLinks);
    }

    private void rollbackUnsetSubjectsOfTeacher(JSONArray localLinks) throws Exception {
        getLocalStorageManager().markEntitiesAsIdle(Contract.TeachersSubjects.HOLDER, localLinks);
    }
}
