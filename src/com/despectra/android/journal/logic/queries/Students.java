package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
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
public class Students extends QueryExecDelegate {



    public Students(DelegatingInterface holderInterface) {
        super(holderInterface);
    }

    public JSONObject getByGroup(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        String localGroupId = request.getString("LOCAL_group_id");
        request.remove("LOCAL_group_id");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalStudents(response, localGroupId);
            Uri notifyUri = Uri.parse(String.format("%s/groups/%s/students", Contract.STRING_URI, localGroupId));
            getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");
        }
        return response;
    }

    public JSONObject addInGroup(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        String localGroupId = request.getString("LOCAL_group_id");
        request.remove("LOCAL_group_id");
        request.put("passwd", Utils.md5("001001"));
        long localUserId = preAddUser(request);
        long localStudentId = preAddStudent(localUserId);
        long localSGLinkId = preAddStudentGroupLink(localStudentId, localGroupId);

        Uri notifyUri = Uri.parse(String.format("%s/groups/%s/students", Contract.STRING_URI, localGroupId));
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            persistStudent(localUserId, localStudentId, localSGLinkId, response);
            getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");
        }
        return response;
    }

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        preDeleteStudents(request);

        Uri notifyUri = Uri.parse(String.format("%s/groups/#/students", Contract.STRING_URI));
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery(action);;

        if (Utils.isApiJsonSuccess(response)) {
            persistStudentsDeletion();
            getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");
        }
        return response;
    }

    private void persistStudentsDeletion() {
        getLocalStorageManager().deleteMarkedEntities(Contract.StudentsGroups.HOLDER);
        getLocalStorageManager().deleteMarkedEntities(Contract.Students.HOLDER);
        getLocalStorageManager().deleteMarkedEntities(Contract.Users.HOLDER);
    }

    private void preDeleteStudents(JSONObject request) throws JSONException {
        JSONArray localStudents = request.getJSONArray("LOCAL_students");
        request.remove("LOCAL_students");
        for (int i = 0; i < localStudents.length(); i++) {
            String localStudentId = localStudents.getString(i);
            Cursor localUser = getContext().getContentResolver().query(Contract.Students.URI_AS_USERS,
                    new String[]{Contract.Users._ID},
                    Contract.Students._ID + " = ?",
                    new String[]{ localStudentId },
                    null);
            localUser.moveToFirst();
            String localUserId = localUser.getString(0);
            getLocalStorageManager().markRowAsDeleting(Contract.Users.HOLDER, localUserId);
            getLocalStorageManager().markRowAsDeleting(Contract.Students.HOLDER, localStudentId);
            Cursor localSGLink = getContext().getContentResolver().query(
                    Contract.StudentsGroups.URI,
                    new String[]{Contract.StudentsGroups._ID},
                    Contract.StudentsGroups.FIELD_STUDENT_ID + " = ?",
                    new String[]{ localStudentId },
                    null);
            localSGLink.moveToFirst();
            String localSGLinkId = localSGLink.getString(0);
            getLocalStorageManager().markRowAsDeleting(Contract.StudentsGroups.HOLDER, localSGLinkId);
        }
    }

    private void persistStudent(long localUserId, long localStudentId, long localSGLinkId, JSONObject response) throws JSONException {
        long remoteUserId = response.getLong("user_id");
        long remoteStudentId = response.getLong("student_id");
        long remoteSGLinkId = response.getLong("student_group_link_id");
        getLocalStorageManager().persistTempRow(Contract.Users.HOLDER, localUserId, remoteUserId);
        getLocalStorageManager().persistTempRow(Contract.Students.HOLDER, localStudentId, remoteStudentId);
        getLocalStorageManager().persistTempRow(Contract.StudentsGroups.HOLDER, localSGLinkId, remoteSGLinkId);
    }

    private long preAddStudentGroupLink(long localStudentId, String localGroupId) {
        ContentValues tempSGLink = new ContentValues();
        tempSGLink.put(Contract.StudentsGroups.FIELD_STUDENT_ID, localStudentId);
        tempSGLink.put(Contract.StudentsGroups.FIELD_GROUP_ID, localGroupId);
        return getLocalStorageManager().insertTempRow(Contract.StudentsGroups.HOLDER, tempSGLink);
    }

    private long preAddStudent(long localUserId) {
        ContentValues tempStudent = new ContentValues();
        tempStudent.put(Contract.Students.FIELD_USER_ID, localUserId);
        return getLocalStorageManager().insertTempRow(Contract.Students.HOLDER, tempStudent);
    }

    private long preAddUser(JSONObject request) throws JSONException {
        ContentValues tempUser = new ContentValues();
        tempUser.put(Contract.Users.FIELD_LOGIN, request.getString("login"));
        tempUser.put(Contract.Users.FIELD_NAME, request.getString("name"));
        tempUser.put(Contract.Users.FIELD_MIDDLENAME, request.getString("middlename"));
        tempUser.put(Contract.Users.FIELD_SURNAME, request.getString("surname"));
        tempUser.put(Contract.Users.FIELD_LEVEL, 2);
        return getLocalStorageManager().insertTempRow(Contract.Users.HOLDER, tempUser);
    }

    private void updateLocalStudents(JSONObject response, String localGroupId) throws JSONException {
        Cursor localStudents = getLocalStorageManager().getResolver().query(
                Uri.parse(Contract.STRING_URI + "/groups/" + localGroupId + "/students"),
                new String[]{Contract.StudentsGroups._ID, Contract.StudentsGroups.REMOTE_ID,
                        Contract.Students._ID, Contract.Students.REMOTE_ID,
                        Contract.Users._ID, Contract.Users.REMOTE_ID},
                Contract.StudentsGroups.FIELD_GROUP_ID + " = ?",
                new String[]{localGroupId},
                null
        );

        JSONArray remoteStudents = response.getJSONArray("students");
        for (int i = 0; i < remoteStudents.length(); i++) {
            JSONObject student = remoteStudents.getJSONObject(i);
            student.put("level", 2);
        }
        Map<Long, Long> insertedUsers = getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localStudents,
                Contract.Users.HOLDER,
                remoteStudents,
                "user_id",
                new String[]{"name", "middlename", "surname", "login", "level"},
                new String[]{Contract.Users.FIELD_NAME, Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_SURNAME,
                        Contract.Users.FIELD_LOGIN, Contract.Users.FIELD_LEVEL}
        );
        for (int i = 0; i < remoteStudents.length(); i++) {
            JSONObject student = remoteStudents.getJSONObject(i);
            student.put("user_id", insertedUsers.get(student.getLong("user_id")));
        }
        Map<Long, Long> insertedStudents = getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localStudents,
                Contract.Students.HOLDER,
                remoteStudents,
                "student_id",
                new String[]{"user_id"},
                new String[]{Contract.Students.FIELD_USER_ID}
        );
        for (int i = 0; i < remoteStudents.length(); i++) {
            JSONObject student = remoteStudents.getJSONObject(i);
            student.put("student_id", insertedStudents.get(student.getLong("student_id")));
            student.put("group_id", localGroupId);
        }
        getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localStudents,
                Contract.StudentsGroups.HOLDER,
                remoteStudents,
                "student_group_link_id",
                new String[]{"student_id", "group_id"},
                new String[]{Contract.StudentsGroups.FIELD_STUDENT_ID, Contract.StudentsGroups.FIELD_GROUP_ID}
        );
    }
}
