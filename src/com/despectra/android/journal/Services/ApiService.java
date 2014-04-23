package com.despectra.android.journal.Services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Data.Contract;
import com.despectra.android.journal.Data.MainProvider;
import com.despectra.android.journal.Data.ProviderUpdater;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Server.ServerAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.*;

/**
 * Created by Dmitry on 25.03.14.
 */
public class ApiService extends Service {
    public static final String PACKAGE = JournalApplication.PACKAGE;

    public static final String ACTION_LOGIN = PACKAGE + ".LOGIN";
    public static final String ACTION_LOGOUT = PACKAGE + ".LOGOUT";
    public static final String ACTION_CHECK_TOKEN = PACKAGE + ".CHECK_TOKEN";
    public static final String ACTION_GET_PROFILE = PACKAGE + ".GET_PROFILE";
    public static final String ACTION_GET_AVATAR = PACKAGE + ".GET_AVATAR";
    public static final String ACTION_SET_SERVER_HOST = PACKAGE + ".SET_HOST";
    public static final String AVATAR_FILENAME = "user_avatar";
    private static final String TAG = "BACKGROUND_SERVICE";

    private static final ArrayDeque<String> API_ACTIONS = new ArrayDeque<String>(Arrays.asList(
            ACTION_LOGIN,
            ACTION_LOGOUT,
            ACTION_CHECK_TOKEN,
            ACTION_GET_PROFILE,
            ACTION_GET_AVATAR));

    public static final int MSG_RESPONSE = 0;
    private static final int MSG_PROGRESS = 1;

    private static ServerAPI mServer;
    private ApiServiceBinder mBinder;
    private ApiServiceHelper mServiceHelper;
    private ProviderUpdater mUpdater;
    private Handler mResponseHandler;

    public ApiService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, String.format("Service OnCreate: %d", hashCode()));
        mServer = ServerAPI.instantiate(getApplicationContext(), getHostFromPreferences());
        mBinder = new ApiServiceBinder();
        JournalApplication application = (JournalApplication)getApplicationContext();
        mServiceHelper = application.getApiServiceHelper();
        mResponseHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (what == MSG_RESPONSE) {
                    Response response = (Response) msg.obj;
                    mServiceHelper.onServiceResponse(response);
                }
                if (what == MSG_PROGRESS) {
                    Pair<String, String> obj = (Pair<String, String>) msg.obj;
                    mServiceHelper.onServiceProgress(obj.first, obj.second);
                }
            }
        };
        mUpdater = new ProviderUpdater(getApplicationContext(), Contract.STRING_URI);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, String.format("Service OnDestroy: %d", hashCode()));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    public void processApiAction(final String senderTag, final ApiServiceHelper.ApiAction action) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Response response = new Response();
                try {
                    JSONObject jsonRequest = action.actionData;
                    JSONObject jsonResponse = new JSONObject();
                    response.senderTag = senderTag;
                    response.initialAction = action;
                    int apiActionCode = action.apiCode;
                    switch (apiActionCode) {
                        case APICodes.ACTION_LOGIN:
                            jsonResponse = mServer.login(jsonRequest);
                            break;
                        case APICodes.ACTION_LOGOUT:
                            jsonResponse = mServer.logout(jsonRequest);
                            break;
                        case APICodes.ACTION_GET_MIN_PROFILE:
                            jsonResponse = mServer.getMinProfile(jsonRequest);
                            //TODO load ava
                            break;
                        case APICodes.ACTION_CHECK_TOKEN:
                            jsonResponse = mServer.checkToken(jsonRequest);
                            break;
                        case APICodes.ACTION_GET_INFO:
                            jsonResponse = mServer.getApiInfo(jsonRequest);
                            break;
                        case APICodes.ACTION_GET_EVENTS:
                            jsonResponse = mServer.getEvents(jsonRequest);
                            if (jsonResponse.has("events")) {
                                Cursor localEvents = getContentResolver().query(
                                        Contract.Events.Remote.URI,
                                        new String[]{Contract.Events.Remote._ID, Contract.Events.Remote.REMOTE_ID},
                                        null,
                                        null,
                                        null
                                );
                                mUpdater.updateEntityWithJSONArray(
                                        ProviderUpdater.MODE_REPLACE,
                                        localEvents,
                                        new Contract.EntityColumnsHolder("Events"),
                                        new Contract.RemoteColumnsHolder("Events"),
                                        jsonResponse.getJSONArray("events"),
                                        "id",
                                        new String[]{"text", "datetime"},
                                        new String[]{Contract.Events.FIELD_TEXT, Contract.Events.FIELD_DATETIME}
                                );
                            }
                            break;
                        case APICodes.ACTION_ADD_GROUP:
                            //prepare entered values
                            String remoteParentId = jsonRequest.getString("parent_group_id");
                            String localParentId = jsonRequest.getString("LOCAL_parent_id");
                            jsonRequest.remove("LOCAL_parent_id");
                            ContentValues group = new ContentValues();
                            group.put(Contract.Groups.FIELD_NAME, jsonRequest.getString("name"));
                            group.put(Contract.Groups.FIELD_PARENT_ID, localParentId);
                            //write in local cache
                            long localId = mUpdater.insertTempRow(Contract.Groups.HOLDER, Contract.Groups.Remote.HOLDER, group);
                            jsonResponse = mServer.addGroup(jsonRequest);
                            if (jsonResponse.has("group_id")) {
                                //persist in cache
                                mUpdater.persistTempRow(Contract.Groups.HOLDER,
                                        Contract.Groups.Remote.HOLDER,
                                        localId,
                                        jsonResponse.getLong("group_id"));
                            }
                            break;
                        case APICodes.ACTION_GET_GROUPS:
                            localParentId = jsonRequest.getString("LOCAL_parent_group_id");
                            jsonRequest.remove("LOCAL_parent_group_id");
                            jsonResponse = mServer.getGroups(jsonRequest);
                            if (jsonResponse.has("groups")) {
                                JSONArray groups = jsonResponse.getJSONArray("groups");
                                Cursor localGroups = getContentResolver().query(
                                        Contract.Groups.Remote.URI,
                                        new String[]{Contract.Groups.Remote._ID, Contract.Groups.Remote.REMOTE_ID},
                                        null,
                                        null,
                                        null
                                );
                                for (int i = 0; i < groups.length(); i++) {
                                    groups.getJSONObject(i).put("parent_id", localParentId);
                                }
                                mUpdater.updateEntityWithJSONArray(
                                        ProviderUpdater.MODE_REPLACE,
                                        localGroups,
                                        Contract.Groups.HOLDER,
                                        Contract.Groups.Remote.HOLDER,
                                        jsonResponse.getJSONArray("groups"),
                                        "id",
                                        new String[]{"name", "parent_id"},
                                        new String[]{Contract.Groups.FIELD_NAME, Contract.Groups.FIELD_PARENT_ID}
                                );
                                //update links to parent ids
                                /*for (int i = 0; i < groups.length(); i++) {
                                    String parentId = groups.getJSONObject(i).getString("parent_id");
                                    String remoteId = groups.getJSONObject(i).getString("id");
                                    if (!parentId.equals("0")) {
                                        ContentValues cv = new ContentValues();
                                        cv.put(Contract.Groups.FIELD_PARENT_ID, mUpdater.getLocalIdByRemote(Contract.Groups.Remote.HOLDER, parentId));
                                        getContentResolver().update(
                                                Contract.Groups.URI,
                                                cv,
                                                Contract.Groups._ID + " = ?",
                                                new String[]{mUpdater.getLocalIdByRemote(Contract.Groups.Remote.HOLDER, remoteId)}
                                        );
                                    }
                                }*/
                            }
                            break;
                        case APICodes.ACTION_DELETE_GROUPS:
                            JSONArray localIds = jsonRequest.getJSONArray("LOCAL_groups");
                            jsonRequest.remove("LOCAL_groups");
                            for (int i = 0; i < localIds.length(); i++) {
                                mUpdater.markRowAsDeleting(Contract.Groups.HOLDER, localIds.getString(i));
                            }
                            jsonResponse = mServer.deleteGroups(jsonRequest);
                            if (jsonResponse.has("success") && jsonResponse.getInt("success") == 1) {
                                mUpdater.deleteMarkedRows(Contract.Groups.HOLDER);
                            }
                            break;
                        case APICodes.ACTION_UPDATE_GROUP:
                            String localGroupId = jsonRequest.getString("LOCAL_id");
                            jsonRequest.remove("LOCAL_id");
                            JSONObject groupData = jsonRequest.getJSONObject("data");
                            localParentId = groupData.getString("LOCAL_parent_id");
                            groupData.remove("LOCAL_parent_id");

                            mUpdater.markRowAsUpdating(Contract.Groups.HOLDER, localGroupId);
                            jsonResponse = mServer.updateGroup(jsonRequest);
                            if (jsonResponse.has("success") && jsonResponse.getInt("success") == 1) {
                                ContentValues updated = new ContentValues();
                                updated.put(Contract.Groups.FIELD_NAME, groupData.getString("name"));
                                updated.put(Contract.Groups.FIELD_PARENT_ID, localParentId);
                                mUpdater.persistUpdatingRow(Contract.Groups.HOLDER, localGroupId, updated);
                            }
                            break;
                        case APICodes.ACTION_GET_STUDENTS_BY_GROUP:
                            localGroupId = jsonRequest.getString("LOCAL_group_id");
                            jsonRequest.remove("LOCAL_group_id");
                            jsonResponse = mServer.getStudentsByGroup(jsonRequest);
                            if (jsonResponse.has("success") && jsonResponse.getInt("success") == 1) {
                                Cursor localStudents = getContentResolver().query(
                                        Uri.parse(Contract.STRING_URI + "/groups_remote/" + localGroupId + "/students_remote"),
                                        new String[]{Contract.StudentsGroups.Remote.REMOTE_ID, Contract.StudentsGroups.Remote._ID,
                                                Contract.Students.Remote.REMOTE_ID, Contract.Students.Remote._ID,
                                                Contract.Users.Remote.REMOTE_ID, Contract.Users.Remote._ID},
                                        Contract.StudentsGroups.FIELD_GROUP_ID + " = ?",
                                        new String[]{localGroupId},
                                        null
                                );
                                JSONArray remoteStudents = jsonResponse.getJSONArray("students");
                                for (int i = 0; i < remoteStudents.length(); i++) {
                                    JSONObject student = remoteStudents.getJSONObject(i);
                                    student.put("level", 2);
                                }
                                Map<String, String> insertedUsers = mUpdater.updateEntityWithJSONArray(
                                        ProviderUpdater.MODE_REPLACE,
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
                                    student.put("user_id", insertedUsers.get(student.getString("user_id")));
                                }
                                Map<String, String> insertedStudents = mUpdater.updateEntityWithJSONArray(
                                        ProviderUpdater.MODE_REPLACE,
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
                                    student.put("student_id", insertedStudents.get(student.getString("student_id")));
                                    student.put("group_id", localGroupId);
                                }
                                mUpdater.updateEntityWithJSONArray(
                                        ProviderUpdater.MODE_REPLACE,
                                        localStudents,
                                        Contract.StudentsGroups.HOLDER,
                                        Contract.StudentsGroups.Remote.HOLDER,
                                        remoteStudents,
                                        "student_group_link_id",
                                        new String[]{"student_id", "group_id"},
                                        new String[]{Contract.StudentsGroups.FIELD_STUDENT_ID, Contract.StudentsGroups.FIELD_GROUP_ID}
                                );
                            }
                            break;
                        case APICodes.ACTION_ADD_STUDENT_IN_GROUP:
                            //add user info in cache
                            ContentValues tempUser = new ContentValues();
                            tempUser.put(Contract.Users.FIELD_LOGIN, jsonRequest.getString("login"));
                            tempUser.put(Contract.Users.FIELD_NAME, jsonRequest.getString("name"));
                            tempUser.put(Contract.Users.FIELD_MIDDLENAME, jsonRequest.getString("middlename"));
                            tempUser.put(Contract.Users.FIELD_SURNAME, jsonRequest.getString("surname"));
                            tempUser.put(Contract.Users.FIELD_LEVEL, 2);
                            long localUserId = mUpdater.insertTempRow(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER, tempUser);
                            //add student info in cache
                            ContentValues tempStudent = new ContentValues();
                            tempStudent.put(Contract.Students.FIELD_USER_ID, localUserId);
                            long localStudentId = mUpdater.insertTempRow(Contract.Students.HOLDER, Contract.Students.Remote.HOLDER, tempStudent);
                            //add student-group link in cache
                            localGroupId = jsonRequest.getString("LOCAL_group_id");
                            jsonRequest.remove("LOCAL_group_id");
                            ContentValues tempSGLink = new ContentValues();
                            tempSGLink.put(Contract.StudentsGroups.FIELD_STUDENT_ID, localStudentId);
                            tempSGLink.put(Contract.StudentsGroups.FIELD_GROUP_ID, localGroupId);
                            long localSGLinkId = mUpdater.insertTempRow(Contract.StudentsGroups.HOLDER, Contract.StudentsGroups.Remote.HOLDER,
                                    tempSGLink);
                            jsonRequest.put("passwd", "001001");

                            Pair<String, String> progress = new Pair<String, String>(senderTag, "cached");
                            mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_PROGRESS, progress));
                            //add to server
                            jsonResponse = mServer.addStudentInGroup(jsonRequest);
                            if (jsonResponse.getInt("success") == 1) {
                                //persist
                                long remoteUserId = jsonResponse.getLong("user_id");
                                long remoteStudentId = jsonResponse.getLong("student_id");
                                long remoteSGLinkId = jsonResponse.getLong("student_group_link_id");
                                mUpdater.persistTempRow(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER,
                                        localUserId, remoteUserId);
                                mUpdater.persistTempRow(Contract.Students.HOLDER, Contract.Students.Remote.HOLDER,
                                        localStudentId, remoteStudentId);
                                mUpdater.persistTempRow(Contract.StudentsGroups.HOLDER, Contract.StudentsGroups.Remote.HOLDER,
                                        localSGLinkId, remoteSGLinkId);
                            }

                            break;
                    }
                    response.responseAction = new ApiServiceHelper.ApiAction(apiActionCode, jsonResponse);
                    mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_RESPONSE, response));
                } catch (Exception ex) {
                    try {
                        response.responseAction = new ApiServiceHelper.ApiAction(
                                -1,
                                new JSONObject(String.format("{\"success\":\"0\", \"error_code\":\"1\", \"error_message\":\"%s\"}", ex.getMessage()))
                        );
                    } catch (JSONException e) {
                    }
                    mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_RESPONSE, response));
                }
            }
        }).start();
    }

    private String getHostFromPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return preferences.getString(JournalApplication.PREFERENCE_KEY_HOST, "");
    }

    public static class Response {
        String senderTag;
        ApiServiceHelper.ApiAction initialAction;
        ApiServiceHelper.ApiAction responseAction;
    }

    class ApiServiceBinder extends Binder {
        ApiService getService() {
            return ApiService.this;
        }
    }

}
