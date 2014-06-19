package com.despectra.android.journal.logic.queries;

import android.database.Cursor;
import com.despectra.android.journal.logic.helper.ApiAction;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Dmitry on 02.06.14.
 */
public class Events extends QueryExecDelegate {
    public Events(DelegatingInterface holderInterface, Map<String, Object> configs) {
        super(holderInterface, configs);
    }

    public JSONObject get(ApiAction action) throws Exception {
        JSONObject response = getApplicationServer().executeGetApiQuery(action);;
        if (response.has("events")) {
            updateLocalEvents(response);
        }
        return response;
    }

    private void updateLocalEvents(JSONObject jsonResponse) throws JSONException {
        Cursor localEvents = getLocalStorageManager().getResolver().query(
                Contract.Events.URI,
                new String[]{Contract.Events._ID, Contract.Events.REMOTE_ID},
                null,
                null,
                null
        );
        getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localEvents,
                Contract.Events.HOLDER,
                jsonResponse.getJSONArray("events"),
                "id",
                new String[]{"text", "datetime"},
                new String[]{Contract.Events.FIELD_TEXT, Contract.Events.FIELD_DATETIME}
        );
    }
}
