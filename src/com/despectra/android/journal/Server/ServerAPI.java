package com.despectra.android.journal.Server;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;
import com.despectra.android.journal.App.JournalApplication;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dmitry on 25.03.14.
 */
public class ServerAPI {
    public static final String AVATAR_FILENAME = "user_avatar";

    public static final int CONN_TIMEOUT_MS = 20000;
    public static final int IO_TIMEOUT_MS = 15000;

    private static final JSONPredicate VALID_HOST_PREDICATE = new JSONPredicate() {
        @Override
        public boolean check(JSONObject json) throws Exception {
            return json.has("ok");
        }
    };

    private static final JSONPredicate SIMPLE_PREDICATE = new JSONPredicate() {
        @Override
        public boolean check(JSONObject json) throws Exception {
            return json.getInt("success") == 1;
        }
    };

    public static final JSONPredicate NO_ERROR_PREDICATE = new JSONPredicate() {
        @Override
        public boolean check(JSONObject json) throws Exception {
            return !(json.has("success") && json.getInt("success") == 0);
        }
    };

    private static ServerAPI mServerInstance;
    private Context mContext;
    private HttpClient mClient;
    private String mHost;
    private String mFullApiPath;

    private ServerAPI(Context context, String host) {
        setHost(host);
        setContext(context);
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, CONN_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(httpParams, IO_TIMEOUT_MS);
        mClient = new DefaultHttpClient(httpParams);
    }

    public  static ServerAPI instantiate(Context context, String host) {
        if (mServerInstance == null) {
            mServerInstance = new ServerAPI(context, host);
        } else {
            mServerInstance.setHost(host);
        }
        return mServerInstance;
    }

    public void setHost(String host) {
        mHost = host;
        mFullApiPath = mHost + "/api/index.php";
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public JSONObject getApiInfo(String host) throws Exception {
        HttpGet request = new HttpGet(String.format("%s/api/index.php?info.getInfo", host));
        String response = doQuery(request);
        return processApiResponse(
                response,
                VALID_HOST_PREDICATE,
                null);
    }

    public JSONObject login(String login, String password) throws Exception {
        String response = doPostApiQuery(
                "auth.login",
                new NameValuePair[]{
                        new BasicNameValuePair("login", login),
                        new BasicNameValuePair("passwd", password)
                });
        return processApiResponse(
                response,
                SIMPLE_PREDICATE,
                null);
    }

    public JSONObject logout(String token) throws Exception {
        JSONObject json = new JSONObject();
        json.put("token", token);
        String response = doGetApiQuery(
                "auth.logout",
                json.toString());
        return processApiResponse(
                response,
                SIMPLE_PREDICATE,
                null);
    }

    public JSONObject checkToken(String token) throws Exception {
        JSONObject json = new JSONObject();
        json.put("token", token);
        String response = doGetApiQuery(
                "auth.checktoken",
                json.toString());
        return processApiResponse(
                response,
                SIMPLE_PREDICATE,
                null);
    }

    public JSONObject getMinProfile(String token) throws Exception {
        JSONObject json = new JSONObject();
        json.put("token", token);
        String response = doGetApiQuery(
                "profile.getminprofile",
                json.toString());
        return processApiResponse(
                response,
                NO_ERROR_PREDICATE,
                new ApiCallback() {
                    @Override
                    public void apiSuccess(JSONObject json) throws JSONException, IOException {
                        String avatarUrl = "http://" + json.getString("avatar");
                        InputStream in = (InputStream) new URL(avatarUrl).getContent();
                        FileOutputStream fos = mContext.openFileOutput(AVATAR_FILENAME, Context.MODE_PRIVATE);
                        int b;
                        while ((b = in.read()) != -1) {
                            fos.write(b);
                        }
                        in.close();
                        fos.close();
                    }
                }
        );
    }

    public JSONObject getEvents(String token, String offset, String count) throws Exception {
        JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("offset", offset);
        json.put("count", count);
        String response = doGetApiQuery(
                "events.getEvents",
                json.toString());
        return processApiResponse(
                response,
                NO_ERROR_PREDICATE,
                null
        );
    }

    public JSONObject addGroup(String token, String name, String parentId) throws Exception {
        JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("name", name);
        json.put("parent_id", parentId);
        String response = doGetApiQuery(
                "groups.addGroup",
                json.toString());
        return processApiResponse(
                response,
                SIMPLE_PREDICATE,
                null);
    }

    public JSONObject getGroups(String token, String parentGroupId, String offset, String count) throws Exception {
        JSONObject json = new JSONObject();
        json.put("token", token);
        json.put("parent_group_id", parentGroupId);
        json.put("offset", offset);
        json.put("count", count);
        String response = doGetApiQuery(
                "groups.getGroups",
                json.toString());
        return processApiResponse(
                response,
                SIMPLE_PREDICATE,
                null);
    }

    private String getHost(){
        String host = PreferenceManager.getDefaultSharedPreferences(mContext).getString(JournalApplication.PREFERENCE_KEY_HOST, "");
        return host;
    }

    private String getFullApiPath() {
        return getHost() + "/api/index.php";
    }

    private String doPostApiQuery(String apiMethod, NameValuePair[] body) throws Exception {
        List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
        requestParams.addAll(Arrays.asList(body));
        HttpPost request = new HttpPost(getFullApiPath() + "?" + apiMethod);
        request.setEntity(new UrlEncodedFormEntity(requestParams, "UTF-8"));
        return doQuery(request);
    }

    private String doGetApiQuery(String apiMethod, String arg) throws Exception {
        arg = URLEncoder.encode(arg, "UTF-8");
        String requestBody = (arg.isEmpty()) ? apiMethod : String.format("%s=%s", apiMethod, arg);
        HttpGet request = new HttpGet(String.format("%s?%s", getFullApiPath(), requestBody));
        return doQuery(request);
    }

    private String doQuery(HttpUriRequest request) throws Exception {
        try {
            //LETS TRY APACHE HTTPClient
            HttpResponse response = mClient.execute(request);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, "UTF-8");
        } catch (Exception ex) {
            throw ex;
        }
    }

    private JSONObject processApiResponse(
            String response,
            JSONPredicate success,
            ApiCallback callback) throws Exception {
        JSONObject json;
        try {
            json = new JSONObject(response);
            boolean isSuccess = success.check(json);
            if (isSuccess && callback != null) {
                callback.apiSuccess(json);
            }
            return json;
        } catch (Exception ex) {
            throw ex;
        }
    }

    private interface JSONPredicate {
        public boolean check(JSONObject json) throws Exception;
    }

    private interface ApiCallback {
        public void apiSuccess(JSONObject data) throws Exception;
    }
}
