package com.despectra.android.journal.utils;

import com.despectra.android.journal.model.EntityIds;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Dmitry on 21.05.14.
 */
public class JSONBuilder {
    private JSONObject mJson;

    public JSONBuilder() {
        mJson = new JSONObject();
    }

    public JSONBuilder addKeyValue(String key, Object value) {
        try {
            mJson.put(key, value);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Wrong arguments for key/value");
        }
        return this;
    }

    public JSONBuilder addArray(String key, Object[] values) {
        try {
            JSONArray array = new JSONArray();
            for (Object v : values) {
                array.put(v);
            }
            mJson.put(key, array);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Wrong arguments for key/array");
        }
        return this;
    }

    public JSONBuilder addArray(String key, long[] values) {
        try {
            JSONArray array = new JSONArray();
            for (Object v : values) {
                array.put(v);
            }
            mJson.put(key, array);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Wrong arguments for key/array");
        }
        return this;
    }

    public JSONBuilder addEntityIds(String key, EntityIds ids) {
        try {
            mJson.put("LOCAL_" + key, ids.getLocalId());
            mJson.put(key, ids.getRemoteId());
        } catch (JSONException e) {

        }
        return this;
    }

    public JSONBuilder addEntityIdsArray(String key, EntityIds[] ids) {
        try {
            JSONArray localArray = new JSONArray();
            JSONArray remoteArray = new JSONArray();
            for (EntityIds itemId : ids) {
                localArray.put(itemId.getLocalId());
                remoteArray.put(itemId.getRemoteId());
            }
            mJson.put("LOCAL_" + key, localArray);
            mJson.put(key, remoteArray);
        } catch (JSONException e) {
        }
        return this;
    }

    public JSONObject create() {
        return mJson;
    }
}
