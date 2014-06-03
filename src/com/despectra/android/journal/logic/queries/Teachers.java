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
public class Teachers {
    public JSONObject get(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONObject response = mServer.executeGetApiQuery("teachers.getTeachers", request);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalTeachers(response);
        }
        return response;
    }

    public JSONObject getOne(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherId = request.getLong("LOCAL_teacher_id");
        request.remove("LOCAL_teacher_id");
        JSONObject response = mServer.executeGetApiQuery("teachers.getTeacher", request);
        if (Utils.isApiJsonSuccess(response)) {
            //update user data
        } else {
            int errorCode = response.getInt("error_code");
            switch (errorCode) {
                case 100:
                    //delete non-existing record
                    deleteLocalTeacher(localTeacherId);
                    break;
                default:
                    break;
            }
            //analyze error code
        }

        return response;
    }

    private void deleteLocalTeacher(long localTeacherId) {
        String[] selectionArgs = new String[]{String.valueOf(localTeacherId)};
        Cursor uidCursor = mContext.getContentResolver().query(Contract.Teachers.URI,
                new String[]{Contract.Users._ID},
                Contract.Teachers.FIELD_USER_ID + " = ?",
                selectionArgs,
                null);
        mContext.getContentResolver().delete(Contract.Teachers.URI,
                Contract.Teachers._ID + " = ?",
                selectionArgs);
        mContext.getContentResolver().delete(Contract.Teachers.Remote.URI,
                Contract.Teachers.Remote._ID + " = ?",
                selectionArgs);
        uidCursor.moveToFirst();
        long uid = uidCursor.getLong(0);
        forUsers().deleteLocalUser(uid);
    }

    public JSONObject add(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;

        long localUserId = preAddUser(request);
        long localTeacherId = preAddTeacher(localUserId);
        //notify helper about caching completed
        Pair<String, String> progress = new Pair<String, String>(action.clientTag, "cached");
        mResponseHandler.sendMessage(Message.obtain(mResponseHandler, ApiService.MSG_PROGRESS, progress));

        JSONObject response = mServer.executeGetApiQuery("teachers.addTeacher", request);
        if (Utils.isApiJsonSuccess(response)) {
            persistTeacher(localUserId, localTeacherId, response);
        }
        return response;
    }

    public JSONObject delete(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        preDeleteTeachers(request);
        mResponseHandler.sendMessage(Message.obtain(mResponseHandler, ApiService.MSG_PROGRESS, new Pair<String, String>(action.clientTag, "cached")));

        JSONObject response = mServer.executeGetApiQuery("teachers.deleteTeachers", request);

        if (Utils.isApiJsonSuccess(response)) {
            persistTeachersDeletion();
        }
        return response;
    }

    private long preAddUser(JSONObject request) throws JSONException {
        ContentValues tempUser = new ContentValues();
        tempUser.put(Contract.Users.FIELD_LOGIN, request.getString("login"));
        tempUser.put(Contract.Users.FIELD_NAME, request.getString("firstName"));
        tempUser.put(Contract.Users.FIELD_MIDDLENAME, request.getString("middleName"));
        tempUser.put(Contract.Users.FIELD_SURNAME, request.getString("secondName"));
        tempUser.put(Contract.Users.FIELD_LEVEL, 4);
        return mLocalStorageManager.insertTempRow(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER, tempUser);
    }

    private long preAddTeacher(long localUserId) {
        ContentValues tempStudent = new ContentValues();
        tempStudent.put(Contract.Teachers.FIELD_USER_ID, localUserId);
        return mLocalStorageManager.insertTempRow(Contract.Teachers.HOLDER, Contract.Teachers.Remote.HOLDER, tempStudent);
    }

    private void persistTeacher(long localUserId, long localTeacherId, JSONObject response) throws JSONException {
        long remoteUserId = response.getLong("user_id");
        long remoteStudentId = response.getLong("teacher_id");
        mLocalStorageManager.persistTempRow(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER,
                localUserId, remoteUserId);
        mLocalStorageManager.persistTempRow(Contract.Teachers.HOLDER, Contract.Teachers.Remote.HOLDER,
                localTeacherId, remoteStudentId);
    }

    private void preDeleteTeachers(JSONObject request) throws JSONException {
        JSONArray localTeachers = request.getJSONArray("LOCAL_teachers");
        request.remove("LOCAL_teachers");
        for (int i = 0; i < localTeachers.length(); i++) {
            String localTeacherId = localTeachers.getString(i);
            Cursor localUser = mContext.getContentResolver().query(Contract.Teachers.URI,
                    new String[]{Contract.Users._ID},
                    Contract.Teachers._ID + " = ?",
                    new String[]{ localTeacherId },
                    null);
            localUser.moveToFirst();
            String localUserId = localUser.getString(0);
            mLocalStorageManager.markRowAsDeleting(Contract.Users.HOLDER, localUserId);
            mLocalStorageManager.markRowAsDeleting(Contract.Teachers.HOLDER, localTeacherId);
        }
    }

    private void persistTeachersDeletion() {
        mLocalStorageManager.deleteMarkedEntities(Contract.Teachers.HOLDER, Contract.Teachers.Remote.HOLDER);
        mLocalStorageManager.deleteMarkedEntities(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER);
    }

    private void updateLocalTeachers(JSONObject response) throws JSONException {
        Cursor localTeachers = mLocalStorageManager.getResolver().query(
                Uri.parse(Contract.STRING_URI + "/teachers"),
                new String[]{Contract.Teachers.Remote.REMOTE_ID, Contract.Teachers.Remote._ID,
                        Contract.Users.Remote.REMOTE_ID, Contract.Users.Remote._ID},
                null,
                null,
                null
        );
        JSONArray remoteTeachers = response.getJSONArray("teachers");
        for (int i = 0; i < remoteTeachers.length(); i++) {
            JSONObject teacher = remoteTeachers.getJSONObject(i);
            teacher.put("level", 4);
        }
        Map<Long, Long> affectedUsers = mLocalStorageManager.updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localTeachers,
                Contract.Users.HOLDER,
                Contract.Users.Remote.HOLDER,
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
        mLocalStorageManager.updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localTeachers,
                Contract.Teachers.HOLDER,
                Contract.Teachers.Remote.HOLDER,
                remoteTeachers,
                "teacher_id",
                new String[]{"user_id"},
                new String[]{Contract.Teachers.FIELD_USER_ID}
        );
    }
}
