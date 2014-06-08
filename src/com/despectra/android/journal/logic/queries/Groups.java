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

/**
* Created by Dmitry on 02.06.14.
*/
public class Groups extends QueryExecDelegate {

    public Groups(DelegatingInterface holderInterface) {
        super(holderInterface);
    }

    /*
    * ****************+++++++++++++++++++ ADDITION +++++++++++++++++**************************
     */

    public JSONObject add(ApiAction action) throws Exception {
        long localId = preAddGroup(action.actionData);
        JSONObject jsonResponse = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(jsonResponse)) {
            //commit
            getLocalStorageManager().persistTempRow(Contract.Groups.HOLDER, localId, jsonResponse.getLong("group_id"));
        } else {
            //rollback
            getLocalStorageManager().deleteEntityByLocalId(Contract.Groups.HOLDER, localId);
        }
        return jsonResponse;
    }

    private long preAddGroup(JSONObject jsonRequest) throws JSONException {
        String localParentId = jsonRequest.getString("LOCAL_parent_id");
        jsonRequest.remove("LOCAL_parent_id");
        ContentValues group = new ContentValues();
        group.put(Contract.Groups.FIELD_NAME, jsonRequest.getString("name"));
        group.put(Contract.Groups.FIELD_PARENT_ID, localParentId);
        //write in local cache
        return getLocalStorageManager().insertTempRow(Contract.Groups.HOLDER, group);
    }

    /*
    ******************^^^^^^^^^^^^^^ RETRIEVING ^^^^^^^^^^^^^^^^^^^************************
     */

    public JSONObject get(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        String localParentId = request.getString("LOCAL_parent_group_id");
        request.remove("LOCAL_parent_group_id");
        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            updateLocalGroups(response, localParentId);
        }
        return response;
    }

    private void updateLocalGroups(JSONObject jsonResponse, String localParentId) throws JSONException {
        JSONArray groups = jsonResponse.getJSONArray("groups");
        Cursor localGroups = getLocalStorageManager().getResolver().query(
                Contract.Groups.URI,
                new String[]{Contract.Groups._ID, Contract.Groups.REMOTE_ID},
                null,
                null,
                null
        );
        for (int i = 0; i < groups.length(); i++) {
            groups.getJSONObject(i).put("parent_id", localParentId);
        }
        getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localGroups,
                Contract.Groups.HOLDER,
                jsonResponse.getJSONArray("groups"),
                "id",
                new String[]{"name", "parent_id"},
                new String[]{Contract.Groups.FIELD_NAME, Contract.Groups.FIELD_PARENT_ID}
        );
    }

    /* TODO
    ***********************~~~~~~~~~~~~~~~ UPDATING ~~~~~~~~~~~~~~~~**********************
     */

    public JSONObject update(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localGroupId = request.getLong("LOCAL_id");
        request.remove("LOCAL_id");
        JSONObject groupData = request.getJSONObject("data");
        String localParentId = groupData.getString("LOCAL_parent_id");
        groupData.remove("LOCAL_parent_id");
        getLocalStorageManager().markRowAsUpdating(Contract.Groups.HOLDER, localGroupId);

        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            ContentValues updated = new ContentValues();
            updated.put(Contract.Groups.FIELD_NAME, groupData.getString("name"));
            updated.put(Contract.Groups.FIELD_PARENT_ID, localParentId);
            getLocalStorageManager().persistUpdatingRow(Contract.Groups.HOLDER, localGroupId, updated);
        } else {
            //rollback
        }
        return response;
    }

    /*
    ************************--------------- DELETION ------------***********************************
     */

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long[] localIds = preDeleteGroups(request);
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().deleteEntitiesByLocalIds(Contract.Groups.HOLDER, localIds);
        } else {
            getLocalStorageManager().markRowsAsIdle(Contract.Groups.HOLDER, localIds);
        }
        return response;
    }

    private long[] preDeleteGroups(JSONObject jsonRequest) throws JSONException {
        JSONArray localIds = jsonRequest.getJSONArray("LOCAL_groups");
        jsonRequest.remove("LOCAL_groups");
        long[] ids = Utils.getIdsFromJSONArray(localIds);
        getLocalStorageManager().markRowsAsDeleting(Contract.Groups.HOLDER, ids);
        return ids;
    }
}
