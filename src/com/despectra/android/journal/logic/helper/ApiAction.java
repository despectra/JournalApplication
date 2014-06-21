package com.despectra.android.journal.logic.helper;

import com.despectra.android.journal.utils.Utils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmitry on 06.06.14.
 */
public class ApiAction {
    public int apiCode;
    public long creationTime;
    public String clientTag;
    public JSONObject actionData;
    public Map<String, Object> extras;
    public String hash;

    public ApiAction(int apiCode, String senderTag, JSONObject actionData) {
        this.apiCode = apiCode;
        this.actionData = actionData;
        this.clientTag = senderTag;
        creationTime = System.currentTimeMillis();
        hash = Utils.md5(String.format("%d%s%s", apiCode, senderTag, actionData.toString()));
    }

    public ApiAction(int code, String senderTag, JSONObject actionData, Map<String, Object> extras) {
        this.apiCode = code;
        this.actionData = actionData;
        this.clientTag = senderTag;
        creationTime = System.currentTimeMillis();
        this.extras = extras;
    }

    public void addExtras(String key, Object value) {
        if (extras == null) {
            extras = new HashMap<String, Object>();
        }
        extras.put(key, value);
    }

    public void setExtras(Map<String, Object> extras) {
        this.extras = extras;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ApiAction)) {
            return false;
        }
        ApiAction action = (ApiAction) o;
        return hash.equals(action.hash);
    }
}
