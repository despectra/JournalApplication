package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.local.TableModel;
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

    public Students(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
        mTable = TableModel.getTable(StudentsGroups.TABLE);
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

    private void updateLocalStudents(JSONObject response, String localGroupId) throws Exception {
        Cursor localStudentsGroups = getLocalStorageManager().getResolver().query(
                StudentsGroups.URI,
                new String[]{StudentsGroups._ID, StudentsGroups.REMOTE_ID},
                StudentsGroups.FIELD_GROUP_ID + " = ?",
                new String[]{String.valueOf(localGroupId)},
                null
        );
        JSONArray remoteStudents = response.getJSONArray("students");
        for (int i = 0; i < remoteStudents.length(); i++) {
            JSONObject student = remoteStudents.getJSONObject(i);
            student.put("level", 2);
            student.put("group_id", localGroupId);
        }
        getLocalStorageManager().updateComplexEntityWithJsonResponse(
                LocalStorageManager.MODE_REPLACE,
                localStudentsGroups,
                mTable,
                remoteStudents,
                null);
    }

    /*
    * ****************+++++++++++++++++++ ADDITION +++++++++++++++++**************************
     */
    public JSONObject addInGroup(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        request.put("passwd", Utils.md5("001001"));
        long localGroupId = request.getLong("LOCAL_group_id");
        request.remove("LOCAL_group_id");
        long localSG = getLocalStorageManager().preInsertEntity(mTable, request);
        Uri notifyUri = Uri.parse(String.format("%s/groups/%d/students", Contract.STRING_URI, localGroupId));
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().commitInsertingEntity(mTable, localSG, response);
        } else {
            getLocalStorageManager().rollbackInsertingEntity(mTable, localSG);
        }
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");
        return response;
    }

    /*
    ************************--------------- DELETION ------------***********************************
     */

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long[] localIds = Utils.getIdsFromJSONArray(request.getJSONArray("LOCAL_stuents"));
        request.remove("LOCAL_students");
        getLocalStorageManager().preDeleteEntitiesCascade(mTable, localIds);
        Uri notifyUri = Uri.parse(String.format("%s/groups/#/students", Contract.STRING_URI));
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");

        JSONObject response = getApplicationServer().executeGetApiQuery(action);

        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().commitDeletingEntitiesCascade(mTable, localIds);
        } else {
            getLocalStorageManager().rollbackDeletingEntityCascade(mTable, localIds);
        }
        getLocalStorageManager().notifyUriForClients(notifyUri, action, "StudentsFragment");
        return response;
    }

    /* TODO
    ***********************~~~~~~~~~~~~~~~ UPDATING ~~~~~~~~~~~~~~~~**********************
     */
}
