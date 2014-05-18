package com.despectra.android.journal.logic.services;

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
import com.despectra.android.journal.Data.ProviderUpdater;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Server.ServerAPI;
import com.despectra.android.journal.Services.ApiServiceHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private final Map<Integer, ActionImpl> mActionsImpls = new HashMap<Integer, ActionImpl>();

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

        createActionsImplementations();
    }

    private void createActionsImplementations() {
        mActionsImpls.put(APICodes.ACTION_LOGIN, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.login(action.actionData);
            }
        });
        mActionsImpls.put(APICodes.ACTION_LOGOUT, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.logout(action.actionData);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_MIN_PROFILE, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.getMinProfile(action.actionData);
                //TODO load ava
            }
        });
        mActionsImpls.put(APICodes.ACTION_CHECK_TOKEN, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.checkToken(action.actionData);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_INFO, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.getApiInfo(action.actionData);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_EVENTS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                Events events = new Events();
                return events.get(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_ADD_GROUP, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                Groups groups = new Groups();
                return groups.add(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_GROUPS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                Groups groups = new Groups();
                return groups.get(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_DELETE_GROUPS, new ActionImpl() {
            @Override
            public JSONObject doAction (ApiServiceHelper.ApiAction action) throws Exception {
                Groups groups = new Groups();
                return groups.delete(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_UPDATE_GROUP, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                Groups groups = new Groups();
                return groups.update(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_STUDENTS_BY_GROUP, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                Students students = new Students();
                return students.getByGroup(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_ADD_STUDENT_IN_GROUP, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                Students students = new Students();
                return students.addInGroup(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_DELETE_STUDENTS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                Students students = new Students();
                return students.delete(action);
            }
        });
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

    public void processApiAction(final ApiServiceHelper.ApiAction action) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Response response = new Response();
                try {
                    response.initialAction = action;
                    int apiActionCode = action.apiCode;

                    JSONObject jsonResponse = doAction(action);

                    /*switch (apiActionCode) {

                        case 100500:
                            long g = jsonRequest.getLong("group");
                            String[] lessonsDates = new String[] {
                                    "10.04",
                                    "15.04",
                                    "16.04",
                                    "19.04",
                                    "21.04",
                                    "23.04"
                            };
                            String[] lessonIds = new String[6];
                            for (int i = 0; i < lessonIds.length; i++) {
                                ContentValues cv = new ContentValues();
                                cv.put(Contract.Lessons.FIELD_DATE, lessonsDates[i]);
                                cv.put(Contract.Lessons.FIELD_GROUP_ID, g);
                                cv.put(Contract.Lessons.FIELD_TITLE, "Generic lesson title");
                                lessonIds[i] = getContentResolver().insert(Contract.Lessons.URI, cv).getLastPathSegment();
                            }

                            Cursor students = getContentResolver().query(Uri.parse(String.format("%s/groups/%d/students", Contract.STRING_URI, g)),
                                    new String[]{Contract.Students._ID + " AS _id",},
                                    Contract.StudentsGroups.FIELD_GROUP_ID + " = ?",
                                    new String[]{String.valueOf(g)},
                                    Contract.Users.FIELD_SURNAME + " ASC");

                            students.moveToFirst();
                            do {
                                String student = students.getString(0);
                                for (int i = 0; i < lessonIds.length; i++) {
                                    String lesson = lessonIds[i];
                                    int mark = (int) Math.random() * 6;
                                    if (mark % 2 == 0) {
                                        mark = 0;
                                    } else {
                                        mark = (int) Math.random() * 3 + 3;
                                    }
                                    ContentValues cv = new ContentValues();
                                    cv.put(Contract.Marks.FIELD_LESSON_ID, lesson);
                                    cv.put(Contract.Marks.FIELD_STUDENT_ID, student);
                                    cv.put(Contract.Marks.FIELD_MARK, -1);
                                    getContentResolver().insert(Contract.Marks.URI, cv);
                                }
                            } while (students.moveToNext());

                            break;
                        case 100599:
                            String markId = jsonRequest.getString("markId");
                            String mark = jsonRequest.getString("mark");
                            ContentValues updCv = new ContentValues();
                            updCv.put(Contract.Marks.ENTITY_STATUS, Contract.STATUS_UPDATING);
                            updCv.put(Contract.Marks.FIELD_MARK, mark);
                            getContentResolver().update(Contract.Marks.URI,
                                    updCv,
                                    Contract.Marks._ID + " = " + markId,
                                    null);
                            //mServer.checkToken(jsonRequest);
                            SystemClock.sleep(7000);
                            ContentValues fnlCv = new ContentValues();
                            fnlCv.put(Contract.Marks.ENTITY_STATUS, Contract.STATUS_IDLE);
                            getContentResolver().update(Contract.Marks.URI, fnlCv, Contract.Marks._ID + " = " + markId, null);
                            break;
                    }*/
                    response.responseAction = new ApiServiceHelper.ApiAction(apiActionCode, action.clientTag, jsonResponse);
                    mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_RESPONSE, response));
                } catch (Exception ex) {
                    try {
                        String msg = ex.getMessage().replace("\"", "");
                        response.responseAction = new ApiServiceHelper.ApiAction(
                                action.apiCode,
                                action.clientTag,
                                new JSONObject(String.format("{\"success\":\"0\", \"error_code\":\"1\", \"error_message\":\"%s\"}", msg))
                        );
                    } catch (Exception e) {
                    }
                    mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_RESPONSE, response));
                }
            }
        }).start();
    }

    private JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
        if (mActionsImpls.containsKey(action.apiCode)) {
            return mActionsImpls.get(action.apiCode).doAction(action);
        } else {
            //No api action with given code
            return new JSONObject("{\"success\" : \"0\", \"error_code\" : \"-1\"}");
        }
    }

    private boolean isJsonSuccess(JSONObject jsonResponse) throws JSONException {
        return jsonResponse.has("success") && jsonResponse.getInt("success") == 1;
    }

    private String getHostFromPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return preferences.getString(JournalApplication.PREFERENCE_KEY_HOST, "");
    }



    private class Events {
        public JSONObject get(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject response = mServer.getEvents(action.actionData);
            if (response.has("events")) {
                updateLocalEvents(response);
            }
            return response;
        }

        private void updateLocalEvents(JSONObject jsonResponse) throws JSONException {
            Cursor localEvents = mUpdater.getResolver().query(
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
    }

    private class Groups {

        public JSONObject add(ApiServiceHelper.ApiAction action) throws Exception {
            long localId = preAddGroup(action.actionData);
            JSONObject jsonResponse = mServer.addGroup(action.actionData);
            if (jsonResponse.has("group_id")) {
                //persist in cache
                mUpdater.persistTempRow(Contract.Groups.HOLDER,
                        Contract.Groups.Remote.HOLDER,
                        localId,
                        jsonResponse.getLong("group_id"));
            }
            return jsonResponse;
        }

        public JSONObject get(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            String localParentId = request.getString("LOCAL_parent_group_id");
            request.remove("LOCAL_parent_group_id");
            JSONObject response = mServer.getGroups(request);
            if (response.has("groups")) {
                updateLocalGroups(response, localParentId);
            }
            return response;
        }

        public JSONObject update(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            String localGroupId = request.getString("LOCAL_id");
            request.remove("LOCAL_id");
            JSONObject groupData = request.getJSONObject("data");
            String localParentId = groupData.getString("LOCAL_parent_id");
            groupData.remove("LOCAL_parent_id");
            mUpdater.markRowAsUpdating(Contract.Groups.HOLDER, localGroupId);

            JSONObject response = mServer.updateGroup(request);
            if (isJsonSuccess(response)) {
                ContentValues updated = new ContentValues();
                updated.put(Contract.Groups.FIELD_NAME, groupData.getString("name"));
                updated.put(Contract.Groups.FIELD_PARENT_ID, localParentId);
                mUpdater.persistUpdatingRow(Contract.Groups.HOLDER, localGroupId, updated);
            }
            return response;
        }

        public JSONObject delete(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            preDeleteGroups(request);
            JSONObject response = mServer.deleteGroups(request);
            if (isJsonSuccess(response)) {
                mUpdater.deleteMarkedEntities(Contract.Groups.HOLDER, Contract.Groups.Remote.HOLDER);
            }
            return response;
        }

        private void preDeleteGroups(JSONObject jsonRequest) throws JSONException {
            JSONArray localIds = jsonRequest.getJSONArray("LOCAL_groups");
            jsonRequest.remove("LOCAL_groups");
            for (int i = 0; i < localIds.length(); i++) {
                mUpdater.markRowAsDeleting(Contract.Groups.HOLDER, localIds.getString(i));
            }
        }

        private long preAddGroup(JSONObject jsonRequest) throws JSONException {
            String localParentId = jsonRequest.getString("LOCAL_parent_id");
            jsonRequest.remove("LOCAL_parent_id");
            ContentValues group = new ContentValues();
            group.put(Contract.Groups.FIELD_NAME, jsonRequest.getString("name"));
            group.put(Contract.Groups.FIELD_PARENT_ID, localParentId);
            //write in local cache
            return mUpdater.insertTempRow(Contract.Groups.HOLDER, Contract.Groups.Remote.HOLDER, group);
        }

        private void updateLocalGroups(JSONObject jsonResponse, String localParentId) throws JSONException {
            JSONArray groups = jsonResponse.getJSONArray("groups");
            Cursor localGroups = mUpdater.getResolver().query(
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
        }
    }

    private class Students {

        public JSONObject getByGroup(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            String localGroupId = request.getString("LOCAL_group_id");
            request.remove("LOCAL_group_id");
            JSONObject response = mServer.getStudentsByGroup(request);
            if (isJsonSuccess(response)) {
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
            mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_PROGRESS, progress));

            JSONObject response = mServer.addStudentInGroup(request);
            if (isJsonSuccess(response)) {
                persistStudent(localUserId, localStudentId, localSGLinkId, response);
            }
            return response;
        }

        public JSONObject delete(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            preDeleteStudents(request);
            mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_PROGRESS, new Pair<String, String>(action.clientTag, "cached")));

            JSONObject response = mServer.deleteStudents(request);

            if (isJsonSuccess(response)) {
                persistStudentsDeletion();
            }
            return response;
        }

        private void persistStudentsDeletion() {
            mUpdater.deleteMarkedEntities(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER);
            mUpdater.deleteMarkedEntities(Contract.Students.HOLDER, Contract.Students.Remote.HOLDER);
            mUpdater.deleteMarkedEntities(Contract.StudentsGroups.HOLDER, Contract.StudentsGroups.Remote.HOLDER);
        }

        private void preDeleteStudents(JSONObject request) throws JSONException {
            JSONArray localStudents = request.getJSONArray("LOCAL_students");
            request.remove("LOCAL_students");
            for (int i = 0; i < localStudents.length(); i++) {
                String localStudentId = localStudents.getString(i);
                Cursor localUser = getContentResolver().query(Contract.Users.URI_STUDENTS,
                        new String[]{Contract.Users._ID},
                        Contract.Students._ID + " = ?",
                        new String[]{ localStudentId },
                        null);
                localUser.moveToFirst();
                String localUserId = localUser.getString(0);
                mUpdater.markRowAsDeleting(Contract.Users.HOLDER, localUserId);
                mUpdater.markRowAsDeleting(Contract.Students.HOLDER, localStudentId);
                Cursor localSGLink = getContentResolver().query(
                        Contract.StudentsGroups.URI,
                        new String[]{Contract.StudentsGroups._ID},
                        Contract.StudentsGroups.FIELD_STUDENT_ID + " = ?",
                        new String[]{ localStudentId },
                        null);
                localSGLink.moveToFirst();
                String localSGLinkId = localSGLink.getString(0);
                mUpdater.markRowAsDeleting(Contract.StudentsGroups.HOLDER, localSGLinkId);
            }
        }

        private void persistStudent(long localUserId, long localStudentId, long localSGLinkId, JSONObject response) throws JSONException {
            long remoteUserId = response.getLong("user_id");
            long remoteStudentId = response.getLong("student_id");
            long remoteSGLinkId = response.getLong("student_group_link_id");
            mUpdater.persistTempRow(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER,
                    localUserId, remoteUserId);
            mUpdater.persistTempRow(Contract.Students.HOLDER, Contract.Students.Remote.HOLDER,
                    localStudentId, remoteStudentId);
            mUpdater.persistTempRow(Contract.StudentsGroups.HOLDER, Contract.StudentsGroups.Remote.HOLDER,
                    localSGLinkId, remoteSGLinkId);
        }

        private long preAddStudentGroupLink(long localStudentId, String localGroupId) {
            ContentValues tempSGLink = new ContentValues();
            tempSGLink.put(Contract.StudentsGroups.FIELD_STUDENT_ID, localStudentId);
            tempSGLink.put(Contract.StudentsGroups.FIELD_GROUP_ID, localGroupId);
            return mUpdater.insertTempRow(Contract.StudentsGroups.HOLDER, Contract.StudentsGroups.Remote.HOLDER,
                    tempSGLink);
        }

        private long preAddStudent(long localUserId) {
            ContentValues tempStudent = new ContentValues();
            tempStudent.put(Contract.Students.FIELD_USER_ID, localUserId);
            return mUpdater.insertTempRow(Contract.Students.HOLDER, Contract.Students.Remote.HOLDER, tempStudent);
        }

        private long preAddUser(JSONObject request) throws JSONException {
            ContentValues tempUser = new ContentValues();
            tempUser.put(Contract.Users.FIELD_LOGIN, request.getString("login"));
            tempUser.put(Contract.Users.FIELD_NAME, request.getString("name"));
            tempUser.put(Contract.Users.FIELD_MIDDLENAME, request.getString("middlename"));
            tempUser.put(Contract.Users.FIELD_SURNAME, request.getString("surname"));
            tempUser.put(Contract.Users.FIELD_LEVEL, 2);
            return mUpdater.insertTempRow(Contract.Users.HOLDER, Contract.Users.Remote.HOLDER, tempUser);
        }

        private void updateLocalStudents(JSONObject response, String localGroupId) throws JSONException {
            Cursor localStudents = mUpdater.getResolver().query(
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
    }

    private interface ActionImpl {
        JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception;
    }

    public static class Response {
        ApiServiceHelper.ApiAction initialAction;
        ApiServiceHelper.ApiAction responseAction;
    }

    class ApiServiceBinder extends Binder {
        ApiService getService() {
            return ApiService.this;
        }
    }

}
