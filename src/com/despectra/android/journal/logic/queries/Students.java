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

    /*
    ******************^^^^^^^^^^^^^^ RETRIEVING ^^^^^^^^^^^^^^^^^^^************************
     */
    public JSONObject getByGroup(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        String localGroupId = request.getString("LOCAL_group_id");
        request.remove("LOCAL_group_id");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalStudents(response, localGroupId);
            Uri notifyUri = Uri.parse(String.format("%s/groups/%s/students", Contract.STRING_URI, localGroupId));
            getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");
        }
        return response;
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

    /*
    * ****************+++++++++++++++++++ ADDITION +++++++++++++++++**************************
     */
    public JSONObject addInGroup(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        request.put("passwd", Utils.md5("001001"));
        JSONObject localIds = preAddStudent(request);
        Uri notifyUri = Uri.parse(String.format("%s/groups/%s/students", Contract.STRING_URI, localIds.getString("group_id")));
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            commitAddStudent(localIds, response);
        } else {
            rollbackAddStudent(localIds);
        }
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");
        return response;
    }

    private JSONObject preAddStudent(JSONObject request) throws Exception {
        long localGroupId = request.getLong("LOCAL_group_id");
        request.remove("LOCAL_group_id");

        long localUserId = Users.preAddUser(this, request, 2);

        ContentValues tempStudent = new ContentValues();
        tempStudent.put(Contract.Students.FIELD_USER_ID, localUserId);
        long localStudentId = getLocalStorageManager().insertTempEntity(Contract.Students.HOLDER, tempStudent);

        ContentValues tempSGLink = new ContentValues();
        tempSGLink.put(Contract.StudentsGroups.FIELD_STUDENT_ID, localStudentId);
        tempSGLink.put(Contract.StudentsGroups.FIELD_GROUP_ID, localGroupId);
        long localSGLinkId = getLocalStorageManager().insertTempEntity(Contract.StudentsGroups.HOLDER, tempSGLink);
        JSONObject localIds = new JSONObject();
        localIds.put("user_id", localUserId);
        localIds.put("student_id", localStudentId);
        localIds.put("sg_link_id", localSGLinkId);
        localIds.put("group_id", localGroupId);
        return localIds;
    }

    private void commitAddStudent(JSONObject localIds, JSONObject response) throws JSONException {
        long remoteUserId = response.getLong("user_id");
        long remoteStudentId = response.getLong("student_id");
        long remoteSGLinkId = response.getLong("student_group_link_id");

        Users.commitAddUser(this, localIds.getLong("user_id"), remoteUserId);
        getLocalStorageManager().persistTempEntity(Contract.Students.HOLDER, localIds.getLong("student_id"), remoteStudentId);
        getLocalStorageManager().persistTempEntity(Contract.StudentsGroups.HOLDER, localIds.getLong("sg_link_id"), remoteSGLinkId);
    }

    private void rollbackAddStudent(JSONObject localIds) throws Exception {
        Users.rollbackAddUser(this, localIds.getLong("user_id"));
        getLocalStorageManager().deleteEntityByLocalId(Contract.Students.HOLDER, localIds.getLong("student_id"));
        getLocalStorageManager().deleteEntityByLocalId(Contract.StudentsGroups.HOLDER, localIds.getLong("sg_link_id"));
    }

    /*
    ************************--------------- DELETION ------------***********************************
     */

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;

        JSONArray localIdsArray = preDeleteStudents(request);
        Uri notifyUri = Uri.parse(String.format("%s/groups/#/students", Contract.STRING_URI));
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery(action);

        if (Utils.isApiJsonSuccess(response)) {
            commitDeleteStudents(localIdsArray);
        } else {
            rollbackDeleteStudents(localIdsArray);
        }
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");
        return response;
    }

    private JSONArray preDeleteStudents(JSONObject request) throws JSONException {
        JSONArray localStudents = request.getJSONArray("LOCAL_students");
        request.remove("LOCAL_students");
        JSONArray localIdsArray = new JSONArray();
        for (int i = 0; i < localStudents.length(); i++) {
            JSONObject localIds = new JSONObject();
            long localStudentId = localStudents.getLong(i);
            Cursor localUser = getContext().getContentResolver().query(Contract.Students.URI_AS_USERS,
                    new String[]{Contract.Users._ID},
                    Contract.Students._ID + " = ?",
                    new String[]{String.valueOf(localStudentId)},
                    null);
            localUser.moveToFirst();
            long localUserId = localUser.getLong(0);
            Users.preDeleteUser(this, localUserId);
            getLocalStorageManager().markEntityAsDeleting(Contract.Students.HOLDER, localStudentId);
            Cursor localSGLink = getContext().getContentResolver().query(
                    Contract.StudentsGroups.URI,
                    new String[]{Contract.StudentsGroups._ID},
                    Contract.StudentsGroups.FIELD_STUDENT_ID + " = ?",
                    new String[]{String.valueOf(localStudentId)},
                    null);
            localSGLink.moveToFirst();
            long localSGLinkId = localSGLink.getLong(0);
            getLocalStorageManager().markEntityAsDeleting(Contract.StudentsGroups.HOLDER, localSGLinkId);
            localIds.put("user_id", localUserId);
            localIds.put("student_id", localStudentId);
            localIds.put("sg_link_id", localSGLinkId);
            localIdsArray.put(localIds);
        }
        return localIdsArray;
    }

    private void commitDeleteStudents(JSONArray localIds) throws Exception {
        for (int i = 0; i < localIds.length(); i++) {
            JSONObject ids = localIds.getJSONObject(i);
            getLocalStorageManager().deleteEntityByLocalId(Contract.StudentsGroups.HOLDER, ids.getLong("sg_link_id"));
            getLocalStorageManager().deleteEntityByLocalId(Contract.Students.HOLDER, ids.getLong("student_id"));
            Users.commitDeleteUser(this, ids.getLong("user_id"));
        }
    }

    private void rollbackDeleteStudents(JSONArray localIds) throws Exception {
        for (int i = 0; i < localIds.length(); i++) {
            JSONObject ids = localIds.getJSONObject(i);
            getLocalStorageManager().markEntityAsIdle(Contract.StudentsGroups.HOLDER, ids.getLong("sg_link_id"));
            getLocalStorageManager().markEntityAsIdle(Contract.Students.HOLDER, ids.getLong("student_id"));
            Users.rollbackDeleteUser(this, ids.getLong("user_id"));
        }
    }

    /* TODO
    ***********************~~~~~~~~~~~~~~~ UPDATING ~~~~~~~~~~~~~~~~**********************
     */
}
