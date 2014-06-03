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
public class Subjects extends QueryExecDelegate {

    public Subjects(DelegatingInterface holderInterface) {
        super(holderInterface);
    }

    public JSONObject add(ApiServiceHelper.ApiAction action) throws Exception {
        long localId = preAddSubject(action.actionData);
        JSONObject jsonResponse = getApplicationServer().executeGetApiQuery("subjects.addSubject", action.actionData);
        if (jsonResponse.has("subject_id")) {
            //persist in cache
            getLocalStorageManager().persistTempRow(Contract.Subjects.HOLDER,
                    Contract.Subjects.Remote.HOLDER,
                    localId,
                    jsonResponse.getLong("subject_id"));
        }
        return jsonResponse;
    }

    public JSONObject get(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.getSubjects", request);
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
        getLocalStorageManager().markRowAsUpdating(Contract.Subjects.HOLDER, localSubjId);

        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.updateSubject", request);
        if (Utils.isApiJsonSuccess(response)) {
            ContentValues updated = new ContentValues();
            updated.put(Contract.Subjects.FIELD_NAME, subjectData.getString("name"));
            getLocalStorageManager().persistUpdatingRow(Contract.Subjects.HOLDER, localSubjId, updated);
        }
        return response;
    }

    public JSONObject delete(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject request = action.actionData;
        preDeleteSubject(request);
        JSONObject response = getApplicationServer().executeGetApiQuery("subjects.deleteSubjects", request);
        if (Utils.isApiJsonSuccess(response)) {
            getLocalStorageManager().deleteMarkedEntities(Contract.Subjects.HOLDER, Contract.Subjects.Remote.HOLDER);
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
        return getLocalStorageManager().insertTempRow(Contract.Subjects.HOLDER, Contract.Subjects.Remote.HOLDER, subj);
    }

    private void updateLocalSubjects(JSONObject response) throws Exception {
        JSONArray subjects = response.getJSONArray("subjects");
        Cursor localGroups = getLocalStorageManager().getResolver().query(
                Contract.Subjects.Remote.URI,
                new String[]{Contract.Subjects.Remote._ID, Contract.Subjects.Remote.REMOTE_ID},
                null,
                null,
                null
        );
        getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
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
