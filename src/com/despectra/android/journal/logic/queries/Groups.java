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
public class Groups extends QueryExecDelegate {

    private Contract.EntityTable mTable;

    public Groups(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
        mTable = TableModel.getTable(Contract.Groups.TABLE);
    }

    /*
    * ****************+++++++++++++++++++ ADDITION +++++++++++++++++**************************
     */

    public JSONObject add(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long localId = getLocalStorageManager().preInsertEntity(mTable, request);
        JSONObject jsonResponse = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(jsonResponse)) {
            //commit
            getLocalStorageManager().commitInsertingEntity(mTable, localId, jsonResponse);
        } else {
            //rollback
            getLocalStorageManager().rollbackInsertingEntity(mTable, localId);
        }
        return jsonResponse;
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

    private void updateLocalGroups(JSONObject jsonResponse, String localParentId) throws Exception {
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
        getLocalStorageManager().updateComplexEntityWithJsonResponse(LocalStorageManager.MODE_REPLACE,
                localGroups,
                mTable,
                groups,
                null
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
        getLocalStorageManager().preUpdateEntity(mTable, localGroupId);

        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().commitUpdatingEntity(mTable, localGroupId,
                    new JSONBuilder()
                        .addKeyValue("name", groupData.getString("name"))
                        .addKeyValue("parent_id", localParentId).create());
        } else {
            //rollback
            getLocalStorageManager().rollbackUpdatingEntity(mTable, localGroupId);
        }
        return response;
    }

    /*
    ************************--------------- DELETION ------------***********************************
     */

    public JSONObject delete(ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        long[] localIds = Utils.getIdsFromJSONArray(request.getJSONArray("LOCAL_groups"));
        request.remove("LOCAL_groups");
        getLocalStorageManager().preDeleteEntitiesCascade(mTable, localIds);

        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().commitDeletingEntitiesCascade(mTable, localIds);
        } else {
            getLocalStorageManager().rollbackDeletingEntityCascade(mTable, localIds);
        }
        return response;
    }
}
