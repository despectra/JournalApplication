package com.despectra.android.journal.logic.helper;

import org.json.JSONObject;

/**
 * Created by Dmitry on 06.06.14.
 */
public class ApiAction {
    public int apiCode;
    public JSONObject actionData;
    public long creationTime;
    public String clientTag;

    public ApiAction(int apiCode, String senderTag, JSONObject actionData) {
        this.apiCode = apiCode;
        this.actionData = actionData;
        this.clientTag = senderTag;
        creationTime = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ApiAction)) {
            return false;
        }
        ApiAction action = (ApiAction) o;
        return apiCode == action.apiCode && clientTag.equals(action.clientTag) && actionData.equals(action.actionData);
    }
}
