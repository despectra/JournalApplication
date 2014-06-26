package com.despectra.android.journal.logic.queries;

import android.database.Cursor;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.local.TableModel;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Dmitry on 02.06.14.
 */
public class Events extends QueryExecDelegate {

    private Contract.EntityTable mTable;

    public Events(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
        mTable = TableModel.getTable(Contract.Events.TABLE);
    }

    public JSONObject get(ApiAction action) throws Exception {
        JSONObject response = getApplicationServer().executeGetApiQuery(action);
        if (response.has("events")) {
            updateLocalEvents(response);
        }
        return response;
    }

    private void updateLocalEvents(JSONObject jsonResponse) throws Exception {
        Cursor localEvents = getLocalStorageManager().getResolver().query(
                Contract.Events.URI,
                new String[]{Contract.Events._ID, Contract.Events.REMOTE_ID},
                null,
                null,
                null
        );
        getLocalStorageManager().updateComplexEntityWithJsonResponse(LocalStorageManager.MODE_REPLACE,
                localEvents,
                mTable,
                jsonResponse.getJSONArray("events"),
                null
        );
    }
}
