package com.despectra.android.journal.logic.net;

import com.despectra.android.journal.logic.helper.ApiAction;
import org.apache.http.NameValuePair;
import org.json.JSONObject;

/**
 * Created by Dmitry on 18.05.14.
 */
public interface ApplicationServer {
    public JSONObject getServerInfo(String host) throws Exception;
    public JSONObject executeGetApiQuery(ApiAction action) throws Exception;
    public JSONObject executePostApiQuery(ApiAction action) throws Exception;
}
