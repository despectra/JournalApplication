package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.local.TableModel;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import com.despectra.android.journal.utils.JSONBuilder;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
* Created by Dmitry on 02.06.14.
*/
public class Subjects extends QueryExecDelegate {

    private Contract.EntityTable mSubjectsTable;
    private Contract.EntityTable mTSGTable;

    public Subjects(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
        mTSGTable = TableModel.getTable(Contract.TSG.TABLE);
        mSubjectsTable = TableModel.getTable(Contract.Subjects.TABLE);
    }

    //#####################     ADDITION      ############################################


    public JSONObject add(ApiAction action) throws Exception {
        long localId = getLocalStorageManager().preInsertEntity(mSubjectsTable, action.actionData);
        JSONObject jsonResponse = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(jsonResponse)) {
            //commit
            getLocalStorageManager().commitInsertingEntity(mSubjectsTable, localId, jsonResponse);
        } else {
            //rollback
            getLocalStorageManager().deleteEntityByLocalId(mSubjectsTable, localId);
        }
        return jsonResponse;
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
        JSONArray remoteSubjects = response.getJSONArray("subjects");
        Cursor localSubjects = getLocalStorageManager().getResolver().query(
                Contract.Subjects.URI,
                new String[]{Contract.Subjects._ID, Contract.Subjects.REMOTE_ID},
                null,
                null,
                null
        );
        getLocalStorageManager().updateComplexEntityWithJsonResponse(LocalStorageManager.MODE_REPLACE,
                localSubjects,
                mSubjectsTable,
                remoteSubjects,
                null
        );
    }

    //#################       UPDATING       ##################

    public JSONObject update(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localSubjId = request.getLong("LOCAL_id");
        request.remove("LOCAL_id");
        JSONObject updatingData = request.getJSONObject("data");
        getLocalStorageManager().preUpdateEntity(mSubjectsTable, localSubjId);

        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().commitUpdatingEntity(mSubjectsTable, localSubjId, updatingData);
        } else {
            getLocalStorageManager().rollbackUpdatingEntity(mSubjectsTable, localSubjId);
        }
        return response;
    }

    //#######################      DELETION      #####################################3

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long[] deletingIds = Utils.getIdsFromJSONArray(request.getJSONArray("LOCAL_subjects"));
        request.remove("LOCAL_subjects");
        getLocalStorageManager().preDeleteEntitiesCascade(mSubjectsTable, deletingIds);
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().commitDeletingEntitiesCascade(mSubjectsTable, deletingIds);
        } else {
            getLocalStorageManager().rollbackDeletingEntityCascade(mSubjectsTable, deletingIds);
        }
        return response;
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
            JSONObject item = remoteLinks.getJSONObject(i);
            item.put("teacher_subject_id", forAllTS
                    ? getLocalStorageManager().getLocalIdByRemote(
                            Contract.TeachersSubjects.HOLDER,
                            item.getLong("teacher_subject_id"))
                    : localTeacherSubjectId);
            item.put("group_id",
                    getLocalStorageManager().getLocalIdByRemote(
                            Contract.Groups.HOLDER,
                            item.getLong("group_id")));
        }
        getLocalStorageManager().updateComplexEntityWithJsonResponse(LocalStorageManager.MODE_REPLACE,
                localLinks,
                mTSGTable,
                remoteLinks,
                null
        );
    }

    //###########   SETTING   ###########

    public JSONObject setGroupsOfTeachersSubject(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localTeacherSubjectId = request.getLong("LOCAL_teacher_subject_id");
        long[] localGroupsIds = Utils.getIdsFromJSONArray(request.getJSONArray("LOCAL_groups_ids"));
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
            rollbackSetGroupsOfSubject(localLinksIds);
        }
        getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                action,
                "GroupsForSubjectFragment");
        return response;
    }

    private long[] preSetGroupsForSubject(long localTeacherSubjectId, long[] localGroupsIds) throws Exception {
        long[] preInsertedIds = new long[localGroupsIds.length];
        for (int i = 0; i < localGroupsIds.length; i++) {
            JSONObject insertingRow = new JSONBuilder()
                .addKeyValue("group_id", localGroupsIds[i])
                .addKeyValue("teacher_subject_id", localTeacherSubjectId).create();
            preInsertedIds[i] = getLocalStorageManager().preInsertEntity(mTSGTable, insertingRow);
        }
        return preInsertedIds;
    }

    private void commitSetGroupsOfSubject(long[] localTempIds, JSONArray affectedIds) throws Exception {
        for (int i = 0; i < localTempIds.length; i++) {
            getLocalStorageManager().commitInsertingEntity(mTSGTable,
                    localTempIds[i],
                    new JSONBuilder().addKeyValue("teacher_student_group_id", affectedIds.getLong(i)).create());
        }
    }

    private void rollbackSetGroupsOfSubject(long[] localLinksIds) throws Exception {
        for (int i = 0; i < localLinksIds.length; i++) {
            getLocalStorageManager().rollbackInsertingEntity(mTSGTable, localLinksIds[i]);
        }
    }

    //##########   UNSETTING    ###########

    public JSONObject unsetGroupsOfTeachersSubject(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long[] localLinks = Utils.getIdsFromJSONArray(request.getJSONArray("LOCAL_links_ids"));
        request.remove("LOCAL_links_ids");

        getLocalStorageManager().preDeleteEntitiesCascade(mTSGTable, localLinks);
        getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                action,
                "GroupsForSubjectFragment");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            //commit
            getLocalStorageManager().commitDeletingEntitiesCascade(mTSGTable, localLinks);
        } else {
            //rollback
            getLocalStorageManager().rollbackDeletingEntityCascade(mTSGTable, localLinks);
        }
        getLocalStorageManager().notifyUriForClients(Contract.TSG.URI_WITH_GROUPS,
                action,
                "GroupsForSubjectFragment");
        return response;
    }
}
