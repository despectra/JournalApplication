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
public class Subjects extends QueryExecDelegate {

    public Subjects(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
    }

    //#####################     ADDITION      ############################################


    public JSONObject add(ApiAction action) throws Exception {
        long localId = preAddSubject(action.actionData);
        JSONObject jsonResponse = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(jsonResponse)) {
            //commit
            getLocalStorageManager().persistTempEntity(Contract.Subjects.HOLDER, localId, jsonResponse.getLong("subject_id"));
        } else {
            //rollback
            getLocalStorageManager().deleteEntityByLocalId(Contract.Subjects.HOLDER, localId);
        }
        return jsonResponse;
    }

    private long preAddSubject(JSONObject jsonRequest) throws Exception {
        ContentValues subj = new ContentValues();
        subj.put(Contract.Subjects.FIELD_NAME, jsonRequest.getString("name"));
        return getLocalStorageManager().insertTempEntity(Contract.Subjects.HOLDER, subj);
    }

    //##################     RETRIEVING     ############################

    public JSONObject get(ApiAction action) throws Exception {
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalSubjects(response);
        }
        return response;
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

    //#################       UPDATING       ##################

    public JSONObject update(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localSubjId = request.getLong("LOCAL_id");
        request.remove("LOCAL_id");
        JSONObject subjectData = request.getJSONObject("data");
        getLocalStorageManager().markEntityAsUpdating(Contract.Subjects.HOLDER, localSubjId);

        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            ContentValues updated = new ContentValues();
            updated.put(Contract.Subjects.FIELD_NAME, subjectData.getString("name"));
            getLocalStorageManager().persistUpdatingEntity(Contract.Subjects.HOLDER, localSubjId, updated);
        }
        return response;
    }

    //#######################      DELETION      #####################################3

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONArray deletingIds = request.getJSONArray("LOCAL_subjects");
        preDeleteSubject(request);
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().deleteMarkedEntities(Contract.Subjects.HOLDER);
        } else {
            getLocalStorageManager().markEntitiesAsIdle(Contract.Subjects.HOLDER, Utils.getIdsFromJSONArray(deletingIds));
        }
        return response;
    }

    private void preDeleteSubject(JSONObject jsonRequest) throws JSONException {
        JSONArray localIds = jsonRequest.getJSONArray("LOCAL_subjects");
        jsonRequest.remove("LOCAL_subjects");
        getLocalStorageManager().markEntitiesAsDeleting(Contract.Subjects.HOLDER, Utils.getIdsFromJSONArray(localIds));
    }

    //GROUPS OF TEACHER'S SUBJECT SECTION
    //
    // ###########  RETRIEVING  #########

    public JSONObject getGroupsOfAllTeachersSubjects(ApiAction action) throws Exception {
        return getGroupsOfTeachersSubjectImpl(action, true);
    }

    public JSONObject getGroupsOfTeachersSubject(ApiAction action) throws Exception {
        return getGroupsOfTeachersSubjectImpl(action, false);
    }

    private JSONObject getGroupsOfTeachersSubjectImpl(ApiAction action, boolean forAllTS) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherSubjectId = 0;
        if (!forAllTS) {
            localTeacherSubjectId = request.getLong("LOCAL_teacher_subject_id");
            request.remove("LOCAL_teacher_subject_id");
        }

        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalGroupsLinks(localTeacherSubjectId, response, forAllTS);
            if (!forAllTS) {
                getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS, action, "GroupsForSubjectFragment");
            }
        }
        return response;
    }

    private void updateLocalGroupsLinks(long localTeacherSubjectId, JSONObject response, boolean forAllTS) throws Exception {
        String selection = forAllTS ? null : Contract.TSG.FIELD_TEACHER_SUBJECT_ID + " = ?";
        String[] selectionArgs = forAllTS ? null : new String[]{String.valueOf(localTeacherSubjectId)};
        Cursor localLinks = getLocalStorageManager().getResolver().query(
                Contract.TSG.URI,
                new String[]{Contract.TSG._ID, Contract.TSG.REMOTE_ID},
                selection,
                selectionArgs,
                null);
        JSONArray remoteLinks = response.getJSONArray("groups");
        for (int i = 0; i < remoteLinks.length(); i++) {
            JSONObject element = remoteLinks.getJSONObject(i);
            element.put("teacher_subject_id", forAllTS
                    ? getLocalStorageManager().getLocalIdByRemote(Contract.TeachersSubjects.HOLDER, element.getLong("teacher_subject_id"))
                    : localTeacherSubjectId);
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

    //###########   SETTING   ###########

    public JSONObject setGroupsOfTeachersSubject(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherSubjectId = request.getLong("LOCAL_teacher_subject_id");
        JSONArray localGroupsIds = request.getJSONArray("LOCAL_groups_ids");
        request.remove("LOCAL_teacher_subject_id");
        request.remove("LOCAL_groups_ids");

        long[] localLinksIds = preSetGroupsForSubject(localTeacherSubjectId, localGroupsIds);
        getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                action,
                "GroupsForSubjectFragment");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            //commit
            commitSetGroupsOfSubject(localLinksIds, response.getJSONArray("affected_links"));
        } else {
            // rollback
            getLocalStorageManager().deleteEntitiesByLocalIds(Contract.TSG.HOLDER, localLinksIds);
        }
        getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                action,
                "GroupsForSubjectFragment");
        return response;
    }

    private long[] preSetGroupsForSubject(long localTeacherSubjectId, JSONArray localGroupsIds) throws Exception {
        long[] tempIds = new long[localGroupsIds.length()];
        for (int i = 0; i < localGroupsIds.length(); i++) {
            ContentValues values = new ContentValues();
            values.put(Contract.TSG.FIELD_TEACHER_SUBJECT_ID, localTeacherSubjectId);
            values.put(Contract.TSG.FIELD_GROUP_ID, localGroupsIds.getString(i));
            tempIds[i] = getLocalStorageManager().insertTempEntity(Contract.TSG.HOLDER, values);
        }
        return tempIds;
    }

    private void commitSetGroupsOfSubject(long[] localTempIds, JSONArray affectedIds) throws Exception {
        getLocalStorageManager().persistTempEntities(Contract.TSG.HOLDER, localTempIds, affectedIds);
    }

    //##########   UNSETTING    ###########

    public JSONObject unsetGroupsOfTeachersSubject(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONArray localLinks = request.getJSONArray("LOCAL_links_ids");
        request.remove("LOCAL_links_ids");

        preUnsetGroupsOfSubject(localLinks);
        getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                action,
                "GroupsForSubjectFragment");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            commitUnsetGroupsOfSubject(localLinks);
            getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                    action,
                    "GroupsForSubjectFragment");
        } else {
            //rollback
            getLocalStorageManager().markEntitiesAsIdle(Contract.TSG.HOLDER, localLinks);
        }
        return response;
    }

    private void preUnsetGroupsOfSubject(JSONArray localLinks) throws Exception {
        getLocalStorageManager().markEntitiesAsDeleting(Contract.TSG.HOLDER, localLinks);
    }

    private void commitUnsetGroupsOfSubject(JSONArray localLinks) throws Exception {
        getLocalStorageManager().deleteEntitiesByLocalIds(Contract.TSG.HOLDER, localLinks);
    }
}
