package com.despectra.android.journal.logic.queries;

import android.content.ContentValues;
import android.database.Cursor;
import com.despectra.android.journal.logic.ApiServiceHelper;
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

    public JSONObject add(ApiServiceHelper.ApiAction action) throws Exception {
        long localId = preAddGroup(action.actionData);
        JSONObject jsonResponse = getApplicationServer().executeGetApiQuery("groups.addGroup", action.actionData);
        if (jsonResponse.has("group_id")) {
            //persist in cache
            getLocalStorageManager().persistTempRow(Contract.Groups.HOLDER,
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
        JSONObject response = getApplicationServer().executeGetApiQuery("groups.getGroups", request);
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
        getLocalStorageManager().markRowAsUpdating(Contract.Groups.HOLDER, localGroupId);

        JSONObject response = getApplicationServer().executeGetApiQuery("groups.updateGroup", request);
        if (Utils.isApiJsonSuccess(response)) {
            ContentValues updated = new ContentValues();
            updated.put(Contract.Groups.FIELD_NAME, groupData.getString("name"));
            updated.put(Contract.Groups.FIELD_PARENT_ID, localParentId);
            getLocalStorageManager().persistUpdatingRow(Contract.Groups.HOLDER, localGroupId, updated);
        }
        return response;
    }

    public JSONObject delete(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        preDeleteGroups(request);
        JSONObject response = getApplicationServer().executeGetApiQuery("groups.deleteGroups", request);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().deleteMarkedEntities(Contract.Groups.HOLDER, Contract.Groups.Remote.HOLDER);
        }
        return response;
    }

    private void preDeleteGroups(JSONObject jsonRequest) throws JSONException {
        JSONArray localIds = jsonRequest.getJSONArray("LOCAL_groups");
        jsonRequest.remove("LOCAL_groups");
        for (int i = 0; i < localIds.length(); i++) {
            getLocalStorageManager().markRowAsDeleting(Contract.Groups.HOLDER, localIds.getString(i));
        }
    }

    private long preAddGroup(JSONObject jsonRequest) throws JSONException {
        String localParentId = jsonRequest.getString("LOCAL_parent_id");
        jsonRequest.remove("LOCAL_parent_id");
        ContentValues group = new ContentValues();
        group.put(Contract.Groups.FIELD_NAME, jsonRequest.getString("name"));
        group.put(Contract.Groups.FIELD_PARENT_ID, localParentId);
        //write in local cache
        return getLocalStorageManager().insertTempRow(Contract.Groups.HOLDER, Contract.Groups.Remote.HOLDER, group);
    }

    private void updateLocalGroups(JSONObject jsonResponse, String localParentId) throws JSONException {
        JSONArray groups = jsonResponse.getJSONArray("groups");
        Cursor localGroups = getLocalStorageManager().getResolver().query(
                Contract.Groups.Remote.URI,
                new String[]{Contract.Groups.Remote._ID, Contract.Groups.Remote.REMOTE_ID},
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
                Contract.Groups.Remote.HOLDER,
                jsonResponse.getJSONArray("groups"),
                "id",
                new String[]{"name", "parent_id"},
                new String[]{Contract.Groups.FIELD_NAME, Contract.Groups.FIELD_PARENT_ID}
        );
    }
}
