package com.despectra.android.journal.logic.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;
import com.despectra.android.journal.JournalApplication;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private static ServerAPI mServerInstance;
    private Context mContext;
    private HttpClient mClient;
    private String mHost;
    private String mFullApiPath;

    private ServerAPI(Context context, String host) {
        setHost(host);
        setContext(context);
        mClient = AndroidHttpClient.newInstance(System.getProperty("http.agent"));
    }

    public synchronized static ServerAPI instantiate(Context context, String host) {
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

    public JSONObject getApiInfo(JSONObject data) throws Exception {
        HttpGet request = new HttpGet(String.format("%s/api/index.php?info.getInfo", data.getString("host")));
        return new JSONObject(doQuery(request));
    }

    public JSONObject login(JSONObject data) throws Exception {
        return doPostApiQuery(
                "auth.login",
                new NameValuePair[]{
                        new BasicNameValuePair("login", data.getString("login")),
                        new BasicNameValuePair("passwd", data.getString("passwd"))}
        );
    }

    public JSONObject logout(JSONObject data) throws Exception {
        return doGetApiQuery("auth.logout", data.toString());
    }

    public JSONObject checkToken(JSONObject data) throws Exception {
        return doGetApiQuery("auth.checktoken", data.toString());
    }

    public JSONObject getMinProfile(JSONObject data) throws Exception {
        return doGetApiQuery("profile.getminprofile", data.toString());
    }

    public JSONObject getEvents(JSONObject data) throws Exception {
        return doGetApiQuery("events.getEvents", data.toString());
    }

    public JSONObject addGroup(JSONObject data) throws Exception {
        return doGetApiQuery("groups.addGroup", data.toString());
    }

    public JSONObject getGroups(JSONObject data) throws Exception {
        return doGetApiQuery("groups.getGroups", data.toString());
    }

    public JSONObject deleteGroups(JSONObject data) throws Exception {
        return doGetApiQuery("groups.deleteGroups", data.toString());
    }

    public JSONObject updateGroup(JSONObject data) throws Exception {
        return doGetApiQuery("groups.editGroup", data.toString());
    }

    public JSONObject getStudentsByGroup(JSONObject data) throws Exception {
        return doGetApiQuery("students.getByGroup", data.toString());
    }

    public JSONObject addStudentInGroup(JSONObject data) throws Exception {
        return doGetApiQuery("students.addStudentInGroup", data.toString());
    }

    public JSONObject deleteStudents(JSONObject data) throws Exception {
        return doGetApiQuery("students.deleteStudents", data.toString());
    }

    public void loadAvatar(JSONObject json) throws JSONException, IOException {
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

    private String getHost(){
        String host = PreferenceManager.getDefaultSharedPreferences(mContext).getString(JournalApplication.PREFERENCE_KEY_HOST, "");
        return host;
    }

    private String getFullApiPath() {
        return getHost() + "/api/index.php";
    }

    private JSONObject doPostApiQuery(String apiMethod, NameValuePair[] body) throws Exception {
        List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
        requestParams.addAll(Arrays.asList(body));
        HttpPost request = new HttpPost(getFullApiPath() + "?" + apiMethod);
        request.setEntity(new UrlEncodedFormEntity(requestParams, "UTF-8"));
        return new JSONObject(doQuery(request));
    }

    private JSONObject doGetApiQuery(String apiMethod, String arg) throws Exception {
        arg = URLEncoder.encode(arg, "UTF-8");
        String requestBody = (arg.isEmpty()) ? apiMethod : String.format("%s=%s", apiMethod, arg);
        HttpGet request = new HttpGet(String.format("%s?%s", getFullApiPath(), requestBody));
        return new JSONObject(doQuery(request));
    }

    private String doQuery(HttpUriRequest request) throws Exception {
        try {
            if (JournalApplication.DEBUG) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                boolean useXDebug = prefs.getBoolean(JournalApplication.PREFERENCES_KEY_XDEBUG, false);
                if (useXDebug) {
                    request.addHeader(new BasicHeader("Accept", "*/*"));
                    request.addHeader(new BasicHeader("Cache-Control", "no-cache"));
                    request.addHeader(new BasicHeader("Cookie", "XDEBUG_SESSION=PHPSTORM;path=/;"));
                }
            }
            HttpResponse response = mClient.execute(request);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, "UTF-8");
        } catch (Exception ex) {
            throw ex;
        } finally {

        }
    }
}
