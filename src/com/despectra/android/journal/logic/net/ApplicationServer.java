package com.despectra.android.journal.logic.net;

import org.apache.http.NameValuePair;
import org.json.JSONObject;

/**
 * Created by Dmitry on 18.05.14.
 */
public interface ApplicationServer {
    public JSONObject getServerInfo(String host) throws Exception;
    public JSONObject executeGetApiQuery(String method, JSONObject data) throws Exception;
    public JSONObject executePostApiQuery(String method, JSONObject data) throws Exception;
}
