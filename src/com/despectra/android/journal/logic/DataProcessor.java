package com.despectra.android.journal.logic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.ProviderUpdater;
import com.despectra.android.journal.logic.net.WebApiServer;
import com.despectra.android.journal.logic.services.ApiService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Dmitry on 18.05.14.
 */
public class DataProcessor {

    private Context mContext;
    private ApplicationServer mServer;
    private ProviderUpdater mUpdater;
    private Handler mResponseHandler;

    public DataProcessor(Context context, WebApiServer server, Handler responseHandler) {
        mContext = context;
        mServer = server;
        mUpdater = new ProviderUpdater(context, Contract.STRING_URI);
        mResponseHandler = responseHandler;
    }

    public Events forEvents() {
        return new Events();
    }

    public Groups forGroups() {
        return new Groups();
    }

    public Students forStudents() {
        return new Students();
    }

    public Subjects forSubjects() {
        return new Subjects();
    }

    private boolean isJsonSuccess(JSONObject jsonResponse) throws JSONException {
        return jsonResponse.has("success") && jsonResponse.getInt("success") == 1;
    }

    public class Events {
        public JSONObject get(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject response = mServer.executeGetApiQuery("events.getEvents", action.actionData);
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

    public class Groups {

        public JSONObject add(ApiServiceHelper.ApiAction action) throws Exception {
            long localId = preAddGroup(action.actionData);
            JSONObject jsonResponse = mServer.executeGetApiQuery("groups.addGroup", action.actionData);
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
            JSONObject response = mServer.executeGetApiQuery("groups.getGroups", request);
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

            JSONObject response = mServer.executeGetApiQuery("groups.updateGroup", request);
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
            JSONObject response = mServer.executeGetApiQuery("groups.deleteGroups", request);
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

    public class Students {

        public JSONObject getByGroup(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            String localGroupId = request.getString("LOCAL_group_id");
            request.remove("LOCAL_group_id");
            JSONObject response = mServer.executeGetApiQuery("students.getByGroup", request);
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
            mResponseHandler.sendMessage(Message.obtain(mResponseHandler, ApiService.MSG_PROGRESS, progress));

            JSONObject response = mServer.executeGetApiQuery("students.addStudentInGroup", request);
            if (isJsonSuccess(response)) {
                persistStudent(localUserId, localStudentId, localSGLinkId, response);
            }
            return response;
        }

        public JSONObject delete(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            preDeleteStudents(request);
            mResponseHandler.sendMessage(Message.obtain(mResponseHandler, ApiService.MSG_PROGRESS, new Pair<String, String>(action.clientTag, "cached")));

            JSONObject response = mServer.executeGetApiQuery("students.deleteStudents", request);

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
                Cursor localUser = mContext.getContentResolver().query(Contract.Users.URI_STUDENTS,
                        new String[]{Contract.Users._ID},
                        Contract.Students._ID + " = ?",
                        new String[]{ localStudentId },
                        null);
                localUser.moveToFirst();
                String localUserId = localUser.getString(0);
                mUpdater.markRowAsDeleting(Contract.Users.HOLDER, localUserId);
                mUpdater.markRowAsDeleting(Contract.Students.HOLDER, localStudentId);
                Cursor localSGLink = mContext.getContentResolver().query(
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

    public class Subjects {

        public JSONObject add(ApiServiceHelper.ApiAction action) throws Exception {
            long localId = preAddSubject(action.actionData);
            JSONObject jsonResponse = mServer.executeGetApiQuery("subjects.addSubject", action.actionData);
            if (jsonResponse.has("subject_id")) {
                //persist in cache
                mUpdater.persistTempRow(Contract.Subjects.HOLDER,
                        Contract.Subjects.Remote.HOLDER,
                        localId,
                        jsonResponse.getLong("subject_id"));
            }
            return jsonResponse;
        }

        public JSONObject get(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            JSONObject response = mServer.executeGetApiQuery("subjects.getSubjects", request);
            if (response.has("subjects")) {
                updateLocalSubjects(response);
            }
            return response;
        }

        public JSONObject update(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            String localSubjId = request.getString("LOCAL_id");
            request.remove("LOCAL_id");
            JSONObject subjectData = request.getJSONObject("data");
            mUpdater.markRowAsUpdating(Contract.Subjects.HOLDER, localSubjId);

            JSONObject response = mServer.executeGetApiQuery("subjects.updateSubject", request);
            if (isJsonSuccess(response)) {
                ContentValues updated = new ContentValues();
                updated.put(Contract.Subjects.FIELD_NAME, subjectData.getString("name"));
                mUpdater.persistUpdatingRow(Contract.Subjects.HOLDER, localSubjId, updated);
            }
            return response;
        }

        public JSONObject delete(ApiServiceHelper.ApiAction action) throws Exception {
            JSONObject request = action.actionData;
            preDeleteSubject(request);
            JSONObject response = mServer.executeGetApiQuery("subjects.deleteSubjects", request);
            if (isJsonSuccess(response)) {
                mUpdater.deleteMarkedEntities(Contract.Subjects.HOLDER, Contract.Subjects.Remote.HOLDER);
            }
            return response;
        }

        private void preDeleteSubject(JSONObject jsonRequest) throws JSONException {
            JSONArray localIds = jsonRequest.getJSONArray("LOCAL_subjects");
            jsonRequest.remove("LOCAL_subjects");
            for (int i = 0; i < localIds.length(); i++) {
                mUpdater.markRowAsDeleting(Contract.Subjects.HOLDER, localIds.getString(i));
            }
        }

        private long preAddSubject(JSONObject jsonRequest) throws Exception {
            ContentValues subj = new ContentValues();
            subj.put(Contract.Subjects.FIELD_NAME, jsonRequest.getString("name"));
            //write in local cache
            return mUpdater.insertTempRow(Contract.Subjects.HOLDER, Contract.Subjects.Remote.HOLDER, subj);
        }

        private void updateLocalSubjects(JSONObject response) throws Exception {
            JSONArray subjects = response.getJSONArray("subjects");
            Cursor localGroups = mUpdater.getResolver().query(
                    Contract.Subjects.Remote.URI,
                    new String[]{Contract.Subjects.Remote._ID, Contract.Subjects.Remote.REMOTE_ID},
                    null,
                    null,
                    null
            );
            mUpdater.updateEntityWithJSONArray(
                    ProviderUpdater.MODE_REPLACE,
                    localGroups,
                    Contract.Subjects.HOLDER,
                    Contract.Subjects.Remote.HOLDER,
                    subjects,
                    "id",
                    new String[]{"name"},
                    new String[]{Contract.Subjects.FIELD_NAME}
            );
        }
    }
}
