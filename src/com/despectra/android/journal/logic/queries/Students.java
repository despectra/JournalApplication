package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Message;
import android.util.Pair;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.services.ApiService;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
* Created by Dmitry on 02.06.14.
*/
public class Students {

    public JSONObject getByGroup(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        String localGroupId = request.getString("LOCAL_group_id");
        request.remove("LOCAL_group_id");
        JSONObject response = mServer.executeGetApiQuery("students.getByGroup", request);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalStudents(response, localGroupId);
        }
        return response;
    }

    public JSONObject addInGroup(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        String localGroupId = request.getString("LOCAL_group_id");
        request.remove("LOCAL_group_id");
        request.put("passwd", "001001");
        long localUserId = preAddUser(request);
        long localStudentId = preAddStudent(localUserId);
        long localSGLinkId = preAddStudentGroupLink(localStudentId, localGroupId);
        //notify helper about caching completed
        Pair<String, String> progress = new Pair<String, String>(action.clientTag, "cached");
        mResponseHandler.sendMessage(Message.obtain(mResponseHandler, ApiService.MSG_PROGRESS, progress));

        JSONObject response = mServer.executeGetApiQuery("students.addStudentInGroup", request);
        if (Utils.isApiJsonSuccess(response)) {
            persistStudent(localUserId, localStudentId, localSGLinkId, response);
        }
        return response;
    }

    public JSONObject delete(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        preDeleteStudents(request);
        mResponseHandler.sendMessage(Message.obtain(mResponseHandler, ApiService.MSG_PROGRESS, new Pair<String, String>(action.clientTag, "cached")));

        JSONObject response = mServer.executeGetApiQuery("students.deleteStudents", request);

        if (Utils.isApiJsonSuccess(response)) {
            persistStudentsDeletion();
        }
        return response;
    }

    private void persistStudentsDeletion() {
        mLocalStorageManager.deleteMarkedEntities(Contract.StudentsGroups.HOLDER, Contract.StudentsGroups.Remote.HOLDER);
        mLocalStorageManager.deleteMarkedEntities(Contract.Students.HOLDER, Contract.Students.Remote.HOLDER);
        mLocalStorageManager.deleteMarkedEntities(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER);
    }

    private void preDeleteStudents(JSONObject request) throws JSONException {
        JSONArray localStudents = request.getJSONArray("LOCAL_students");
        request.remove("LOCAL_students");
        for (int i = 0; i < localStudents.length(); i++) {
            String localStudentId = localStudents.getString(i);
            Cursor localUser = mContext.getContentResolver().query(Contract.Users.URI_STUDENTS,
                    new String[]{Contract.Users._ID},
                    Contract.Students._ID + " = ?",
                    new String[]{ localStudentId },
                    null);
            localUser.moveToFirst();
            String localUserId = localUser.getString(0);
            mLocalStorageManager.markRowAsDeleting(Contract.Users.HOLDER, localUserId);
            mLocalStorageManager.markRowAsDeleting(Contract.Students.HOLDER, localStudentId);
            Cursor localSGLink = mContext.getContentResolver().query(
                    Contract.StudentsGroups.URI,
                    new String[]{Contract.StudentsGroups._ID},
                    Contract.StudentsGroups.FIELD_STUDENT_ID + " = ?",
                    new String[]{ localStudentId },
                    null);
            localSGLink.moveToFirst();
            String localSGLinkId = localSGLink.getString(0);
            mLocalStorageManager.markRowAsDeleting(Contract.StudentsGroups.HOLDER, localSGLinkId);
        }
    }

    private void persistStudent(long localUserId, long localStudentId, long localSGLinkId, JSONObject response) throws JSONException {
        long remoteUserId = response.getLong("user_id");
        long remoteStudentId = response.getLong("student_id");
        long remoteSGLinkId = response.getLong("student_group_link_id");
        mLocalStorageManager.persistTempRow(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER,
                localUserId, remoteUserId);
        mLocalStorageManager.persistTempRow(Contract.Students.HOLDER, Contract.Students.Remote.HOLDER,
                localStudentId, remoteStudentId);
        mLocalStorageManager.persistTempRow(Contract.StudentsGroups.HOLDER, Contract.StudentsGroups.Remote.HOLDER,
                localSGLinkId, remoteSGLinkId);
    }

    private long preAddStudentGroupLink(long localStudentId, String localGroupId) {
        ContentValues tempSGLink = new ContentValues();
        tempSGLink.put(Contract.StudentsGroups.FIELD_STUDENT_ID, localStudentId);
        tempSGLink.put(Contract.StudentsGroups.FIELD_GROUP_ID, localGroupId);
        return mLocalStorageManager.insertTempRow(Contract.StudentsGroups.HOLDER, Contract.StudentsGroups.Remote.HOLDER,
                tempSGLink);
    }

    private long preAddStudent(long localUserId) {
        ContentValues tempStudent = new ContentValues();
        tempStudent.put(Contract.Students.FIELD_USER_ID, localUserId);
        return mLocalStorageManager.insertTempRow(Contract.Students.HOLDER, Contract.Students.Remote.HOLDER, tempStudent);
    }

    private long preAddUser(JSONObject request) throws JSONException {
        ContentValues tempUser = new ContentValues();
        tempUser.put(Contract.Users.FIELD_LOGIN, request.getString("login"));
        tempUser.put(Contract.Users.FIELD_NAME, request.getString("name"));
        tempUser.put(Contract.Users.FIELD_MIDDLENAME, request.getString("middlename"));
        tempUser.put(Contract.Users.FIELD_SURNAME, request.getString("surname"));
        tempUser.put(Contract.Users.FIELD_LEVEL, 2);
        return mLocalStorageManager.insertTempRow(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER, tempUser);
    }

    private void updateLocalStudents(JSONObject response, String localGroupId) throws JSONException {
        Cursor localStudents = mLocalStorageManager.getResolver().query(
                Uri.parse(Contract.STRING_URI + "/groups_remote/" + localGroupId + "/students_remote"),
                new String[]{Contract.StudentsGroups.Remote.REMOTE_ID, Contract.StudentsGroups.Remote._ID,
                        Contract.Students.Remote.REMOTE_ID, Contract.Students.Remote._ID,
                        Contract.Users.Remote.REMOTE_ID, Contract.Users.Remote._ID},
                Contract.StudentsGroups.FIELD_GROUP_ID + " = ?",
                new String[]{localGroupId},
                null
        );

        JSONArray remoteStudents = response.getJSONArray("students");
        for (int i = 0; i < remoteStudents.length(); i++) {
            JSONObject student = remoteStudents.getJSONObject(i);
            student.put("level", 2);
        }
        Map<Long, Long> insertedUsers = mLocalStorageManager.updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localStudents,
                Contract.Users.HOLDER,
                Contract.Users.Remote.HOLDER,
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
        Map<Long, Long> insertedStudents = mLocalStorageManager.updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localStudents,
                Contract.Students.HOLDER,
                Contract.Students.Remote.HOLDER,
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
        mLocalStorageManager.updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localStudents,
                Contract.StudentsGroups.HOLDER,
                Contract.StudentsGroups.Remote.HOLDER,
                remoteStudents,
                "student_group_link_id",
                new String[]{"student_id", "group_id"},
                new String[]{Contract.StudentsGroups.FIELD_STUDENT_ID, Contract.StudentsGroups.FIELD_GROUP_ID}
        );
    }
}
