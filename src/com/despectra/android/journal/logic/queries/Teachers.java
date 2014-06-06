package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Message;
import android.util.Pair;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import com.despectra.android.journal.logic.services.ApiService;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
* Created by Dmitry on 02.06.14.
*/
public class Teachers extends QueryExecDelegate {
    public Teachers(DelegatingInterface holderInterface) {
        super(holderInterface);
    }

    public JSONObject get(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONObject response = getApplicationServer().executeGetApiQuery("teachers.getTeachers", request);
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
        JSONObject response = getApplicationServer().executeGetApiQuery("teachers.getTeacher", request);
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

    private void deleteLocalTeacher(long localUserId, long localTeacherId) {
        getLocalStorageManager().deleteEntityByLocalId(Contract.Teachers.HOLDER, localTeacherId);
        getLocalStorageManager().deleteEntityByLocalId(Contract.Users.HOLDER, localUserId);
    }

    public JSONObject add(ApiAction action) throws Exception {
        JSONObject request = action.actionData;

        long localUserId = preAddUser(request);
        long localTeacherId = preAddTeacher(localUserId);
        //notify helper about caching completed
        getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery("teachers.addTeacher", request);
        if (Utils.isApiJsonSuccess(response)) {
            persistTeacher(localUserId, localTeacherId, response);
            getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");
        }
        return response;
    }

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        preDeleteTeachers(request);
        getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery("teachers.deleteTeachers", request);
        if (Utils.isApiJsonSuccess(response)) {
            persistTeachersDeletion();
            getLocalStorageManager().notifyUriForClients(Contract.Teachers.URI_AS_USERS, action, "TeachersFragment");
        }
        return response;
    }

    public JSONObject getSubjectsOfTeacher(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherId = request.getLong("LOCAL_teacher_id");
        request.remove("LOCAL_teacher_id");
        JSONObject response = getApplicationServer().executeGetApiQuery("teachers.getSubjectsOfTeacher", request);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalSubjectsLinks(localTeacherId, response);
            getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                    action,
                    "SubjectsOfTeacherFragment");
        }
        return response;
    }

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
        JSONObject response = getApplicationServer().executeGetApiQuery("teachers.setSubjectsOfTeacher", request);
        if (Utils.isApiJsonSuccess(response)) {
            persistSetSubjectsOfTeacher(localLinksIds, response.getJSONArray("affected_links"));
            getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                    action,
                    "SubjectsOfTeacherFragment");
        }
        return response;
    }

    public JSONObject unsetSubjectsOfTeacher(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONArray localLinks = request.getJSONArray("LOCAL_links_ids");
        request.remove("LOCAL_links_ids");

        preUnsetSubjectsOfTeacher(localLinks);
        getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                action,
                "SubjectsOfTeacherFragment");
        JSONObject response = getApplicationServer().executeGetApiQuery("teachers.unsetSubjectsOfTeacher", request);
        if (Utils.isApiJsonSuccess(response)) {
            persistUnsetSubjectsOfTeacher(localLinks);
            getLocalStorageManager().notifyUriForClients(Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                    action,
                    "SubjectsOfTeacherFragment");
        }
        return response;
    }

    private void persistUnsetSubjectsOfTeacher(JSONArray localLinks) throws Exception {
        for (int i = 0; i < localLinks.length(); i++) {
            getLocalStorageManager().deleteEntityByLocalId(Contract.TeachersSubjects.HOLDER, localLinks.getLong(i));
        }
    }

    private void preUnsetSubjectsOfTeacher(JSONArray localLinks) throws Exception {
        for (int i = 0; i < localLinks.length(); i++) {
            getLocalStorageManager().markRowAsDeleting(Contract.TeachersSubjects.HOLDER,
                    localLinks.getString(i));
        }
    }

    private void persistSetSubjectsOfTeacher(long[] localTempIds, JSONArray affectedIds) throws Exception {
        for (int i = 0; i < affectedIds.length(); i++) {
            long localId = localTempIds[i];
            long affectedId = affectedIds.getLong(i);
            getLocalStorageManager().persistTempRow(Contract.TeachersSubjects.HOLDER, localId, affectedId);
        }
    }

    private long[] preSetLocalSubjectsOfTeacher(long localTeacherId, JSONArray localSubjectsIds) throws Exception {
        long[] tempIds = new long[localSubjectsIds.length()];
        for (int i = 0; i < localSubjectsIds.length(); i++) {
            ContentValues values = new ContentValues();
            values.put(Contract.TeachersSubjects.FIELD_TEACHER_ID, localTeacherId);
            values.put(Contract.TeachersSubjects.FIELD_SUBJECT_ID, localSubjectsIds.getString(i));
            tempIds[i] = getLocalStorageManager().insertTempRow(Contract.TeachersSubjects.HOLDER, values);
        }
        return tempIds;
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

    private long preAddUser(JSONObject request) throws JSONException {
        ContentValues tempUser = new ContentValues();
        tempUser.put(Contract.Users.FIELD_LOGIN, request.getString("login"));
        tempUser.put(Contract.Users.FIELD_NAME, request.getString("firstName"));
        tempUser.put(Contract.Users.FIELD_MIDDLENAME, request.getString("middleName"));
        tempUser.put(Contract.Users.FIELD_SURNAME, request.getString("secondName"));
        tempUser.put(Contract.Users.FIELD_LEVEL, 4);
        return getLocalStorageManager().insertTempRow(Contract.Users.HOLDER, tempUser);
    }

    private long preAddTeacher(long localUserId) {
        ContentValues tempStudent = new ContentValues();
        tempStudent.put(Contract.Teachers.FIELD_USER_ID, localUserId);
        return getLocalStorageManager().insertTempRow(Contract.Teachers.HOLDER, tempStudent);
    }

    private void persistTeacher(long localUserId, long localTeacherId, JSONObject response) throws JSONException {
        long remoteUserId = response.getLong("user_id");
        long remoteStudentId = response.getLong("teacher_id");
        getLocalStorageManager().persistTempRow(Contract.Users.HOLDER, localUserId, remoteUserId);
        getLocalStorageManager().persistTempRow(Contract.Teachers.HOLDER, localTeacherId, remoteStudentId);
    }

    private void preDeleteTeachers(JSONObject request) throws JSONException {
        JSONArray localTeachers = request.getJSONArray("LOCAL_teachers");
        request.remove("LOCAL_teachers");
        for (int i = 0; i < localTeachers.length(); i++) {
            String localTeacherId = localTeachers.getString(i);
            Cursor localUser = getContext().getContentResolver().query(Contract.Teachers.URI_AS_USERS,
                    new String[]{Contract.Users._ID},
                    Contract.Teachers._ID + " = ?",
                    new String[]{ localTeacherId },
                    null);
            localUser.moveToFirst();
            String localUserId = localUser.getString(0);
            getLocalStorageManager().markRowAsDeleting(Contract.Users.HOLDER, localUserId);
            getLocalStorageManager().markRowAsDeleting(Contract.Teachers.HOLDER, localTeacherId);
        }
    }

    private void persistTeachersDeletion() {
        getLocalStorageManager().deleteMarkedEntities(Contract.Teachers.HOLDER);
        getLocalStorageManager().deleteMarkedEntities(Contract.Users.HOLDER);
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
}
