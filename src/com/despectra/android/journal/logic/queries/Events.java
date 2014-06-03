package com.despectra.android.journal.logic.queries;

import android.database.Cursor;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.queries.common.DelegatingInterface;
import com.despectra.android.journal.logic.queries.common.QueryExecDelegate;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Dmitry on 02.06.14.
 */
public class Events extends QueryExecDelegate {
    public Events(DelegatingInterface holderInterface) {
        super(holderInterface);
    }

    public JSONObject get(ApiServiceHelper.ApiAction action) throws Exception {
        JSONObject response = getApplicationServer().executeGetApiQuery("events.getEvents", action.actionData);
        if (response.has("events")) {
            updateLocalEvents(response);
        }
        return response;
    }

    private void updateLocalEvents(JSONObject jsonResponse) throws JSONException {
        Cursor localEvents = getLocalStorageManager().getResolver().query(
                Contract.Events.Remote.URI,
                new String[]{Contract.Events.Remote._ID, Contract.Events.Remote.REMOTE_ID},
                null,
                null,
                null
        );
        getLocalStorageManager().updateEntityWithJSONArray(
                LocalStorageManager.MODE_REPLACE,
                localEvents,
                new Contract.EntityColumnsHolder("Events"),
                new Contract.RemoteColumnsHolder("Events"),
                jsonResponse.getJSONArray("events"),
                "id",
                new String[]{"text", "datetime"},
                new String[]{Contract.Events.FIELD_TEXT, Contract.Events.FIELD_DATETIME}
        );
    }
}
