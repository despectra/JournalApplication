package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.JsonReader;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
* Created by Dmitry on 02.06.14.
*/
public class Subjects extends QueryExecDelegate {

    public Subjects(DelegatingInterface holderInterface) {
        super(holderInterface);
    }

    public JSONObject add(ApiAction action) throws Exception {
        long localId = preAddSubject(action.actionData);
        JSONObject jsonResponse = getApplicationServer().executeGetApiQuery("subjects.addSubject", action.actionData);
        if (jsonResponse.has("subject_id")) {
            getLocalStorageManager().persistTempRow(Contract.Subjects.HOLDER, localId, jsonResponse.getLong("subject_id"));
        }
        return jsonResponse;
    }

    public JSONObject get(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.getSubjects", request);
        if (response.has("subjects")) {
            updateLocalSubjects(response);
        }
        return response;
    }

    public JSONObject update(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        String localSubjId = request.getString("LOCAL_id");
        request.remove("LOCAL_id");
        JSONObject subjectData = request.getJSONObject("data");
        getLocalStorageManager().markRowAsUpdating(Contract.Subjects.HOLDER, localSubjId);

        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.updateSubject", request);
        if (Utils.isApiJsonSuccess(response)) {
            ContentValues updated = new ContentValues();
            updated.put(Contract.Subjects.FIELD_NAME, subjectData.getString("name"));
            getLocalStorageManager().persistUpdatingRow(Contract.Subjects.HOLDER, localSubjId, updated);
        }
        return response;
    }

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        preDeleteSubject(request);
        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.deleteSubjects", request);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().deleteMarkedEntities(Contract.Subjects.HOLDER);
        }
        return response;
    }

    private void preDeleteSubject(JSONObject jsonRequest) throws JSONException {
        JSONArray localIds = jsonRequest.getJSONArray("LOCAL_subjects");
        jsonRequest.remove("LOCAL_subjects");
        for (int i = 0; i < localIds.length(); i++) {
            getLocalStorageManager().markRowAsDeleting(Contract.Subjects.HOLDER, localIds.getString(i));
        }
    }

    private long preAddSubject(JSONObject jsonRequest) throws Exception {
        ContentValues subj = new ContentValues();
        subj.put(Contract.Subjects.FIELD_NAME, jsonRequest.getString("name"));
        //write in local cache
        return getLocalStorageManager().insertTempRow(Contract.Subjects.HOLDER, subj);
    }

    private void updateLocalSubjects(JSONObject response) throws Exception {
        JSONArray subjects = response.getJSONArray("subjects");
        Cursor localGroups = getLocalStorageManager().getResolver().query(
                Contract.Subjects.URI,
                new String[]{Contract.Subjects._ID, Contract.Subjects.REMOTE_ID},
                null,
                null,
                null
        );
        getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localGroups,
                Contract.Subjects.HOLDER,
                subjects,
                "id",
                new String[]{"name"},
                new String[]{Contract.Subjects.FIELD_NAME}
        );
    }

    public JSONObject getGroupsOfTeachersSubject(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherSubjectId = request.getLong("LOCAL_ts_link");
        request.remove("LOCAL_ts_link");

        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.getGroupsOfTeachersSubject", request);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalGroupsLinks(localTeacherSubjectId, response);
            getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS, action, "GroupsOfTeachersSubjectFragment");
        }
        return response;
    }

    public JSONObject setGroupsOfTeachersSubject(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherSubjectId = request.getLong("LOCAL_ts_link_id");
        JSONArray localGroupsIds = request.getJSONArray("LOCAL_groups_ids");
        request.remove("LOCAL_ts_link_id");
        request.remove("LOCAL_groups_ids");

        long[] localLinksIds = preSetGroupsForSubject(localTeacherSubjectId, localGroupsIds);
        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.setGroupsOfTeachersSubject", request);
        if (Utils.isApiJsonSuccess(response)) {
            persistSetGroupsOfSubject(localLinksIds, response.getJSONArray("groups"));
        }
        return response;
    }

    public JSONObject unsetGroupsOfTeachersSubject(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONArray localLinks = request.getJSONArray("LOCAL_links_ids");
        request.remove("LOCAL_links_ids");

        preUnsetGroupsOfSubject(localLinks);
        getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                action,
                "GroupsOfTeachersSubjectFragment");
        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.unsetGroupsOfTeachersSubject", request);
        if (Utils.isApiJsonSuccess(response)) {
            persistUnsetGroupsOfSubject(localLinks);
            getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                    action,
                    "GroupsOfTeachersSubjectFragment");
        }
        return response;
    }

    private long[] preSetGroupsForSubject(long localTeacherSubjectId, JSONArray localGroupsIds) throws Exception {
        long[] tempIds = new long[localGroupsIds.length()];
        for (int i = 0; i < localGroupsIds.length(); i++) {
            ContentValues values = new ContentValues();
            values.put(Contract.TSG.FIELD_TEACHER_SUBJECT_ID, localTeacherSubjectId);
            values.put(Contract.TSG.FIELD_GROUP_ID, localGroupsIds.getString(i));
            tempIds[i] = getLocalStorageManager().insertTempRow(Contract.TSG.HOLDER, values);
        }
        return tempIds;
    }

    private void persistSetGroupsOfSubject(long[] localTempIds, JSONArray affectedIds) throws Exception {
        for (int i = 0; i < affectedIds.length(); i++) {
            long localId = localTempIds[i];
            long affectedId = affectedIds.getLong(i);
            getLocalStorageManager().persistTempRow(Contract.TSG.HOLDER, localId, affectedId);
        }
    }

    private void preUnsetGroupsOfSubject(JSONArray localLinks) throws Exception {
        for (int i = 0; i < localLinks.length(); i++) {
            getLocalStorageManager().markRowAsDeleting(Contract.TSG.HOLDER, localLinks.getString(i));
        }
    }

    private void persistUnsetGroupsOfSubject(JSONArray localLinks) throws Exception {
        for (int i = 0; i < localLinks.length(); i++) {
            getLocalStorageManager().deleteEntityByLocalId(Contract.TSG.HOLDER, localLinks.getLong(i));
        }
    }

    private void updateLocalGroupsLinks(long localTeacherSubjectId, JSONObject response) throws Exception {
        Cursor localLinks = getLocalStorageManager().getResolver().query(
                Contract.TSG.URI,
                new String[]{Contract.TSG._ID, Contract.TSG.REMOTE_ID},
                Contract.TSG.FIELD_GROUP_ID + " = ?",
                new String[]{String.valueOf(localTeacherSubjectId)},
                null);
        JSONArray remoteLinks = response.getJSONArray("groups");
        for (int i = 0; i < remoteLinks.length(); i++) {
            JSONObject element = remoteLinks.getJSONObject(i);
            element.put("teacher_subject_id", localTeacherSubjectId);
            long remoteGroupId = element.getLong("group_id");
            long localGroupId = getLocalStorageManager().getLocalIdByRemote(Contract.Groups.HOLDER, remoteGroupId);
            element.put("LOCAL_group_id", localGroupId);
        }
        getLocalStorageManager().updateEntityWithJSONArray(LocalStorageManager.MODE_REPLACE,
                localLinks,
                Contract.TSG.HOLDER,
                remoteLinks,
                "id",
                new String[]{"LOCAL_group_id", "teacher_subject_id"},
                new String[]{Contract.TSG.FIELD_GROUP_ID, Contract.TSG.FIELD_TEACHER_SUBJECT_ID}
        );
    }
}
