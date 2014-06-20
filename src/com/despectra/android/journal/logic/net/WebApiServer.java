package com.despectra.android.journal.logic.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.logic.helper.ApiAction;
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
import java.util.*;

/**
 * Created by Dmitry on 25.03.14.
 */
public class WebApiServer implements ApplicationServer {
    public static final String AVATAR_FILENAME = "user_avatar";

    public static final int CONN_TIMEOUT_MS = 20000;
    public static final int IO_TIMEOUT_MS = 15000;

    private static WebApiServer sServerInstance;
    private Context mContext;
    private HttpClient mClient;
    private String mHost;
    private String mFullApiPath;

    public static final Map<Integer, String> METHODS_MAP = new HashMap<Integer, String>();
    static {
        METHODS_MAP.put(APICodes.ACTION_LOGIN, "auth.login");
        METHODS_MAP.put(APICodes.ACTION_LOGOUT, "auth.logout");
        METHODS_MAP.put(APICodes.ACTION_GET_MIN_PROFILE, "profile.getMinProfile");
        METHODS_MAP.put(APICodes.ACTION_CHECK_TOKEN, "auth.checkToken");
        METHODS_MAP.put(APICodes.ACTION_GET_EVENTS, "events.getEvents");
        METHODS_MAP.put(APICodes.ACTION_ADD_GROUP, "groups.addGroup");
        METHODS_MAP.put(APICodes.ACTION_GET_GROUPS, "groups.getGroups");
        METHODS_MAP.put(APICodes.ACTION_DELETE_GROUPS, "groups.deleteGroups");
        METHODS_MAP.put(APICodes.ACTION_UPDATE_GROUP, "groups.updateGroup");
        METHODS_MAP.put(APICodes.ACTION_GET_STUDENTS_BY_GROUP, "students.getByGroup");
        METHODS_MAP.put(APICodes.ACTION_ADD_STUDENT_IN_GROUP, "students.addStudentInGroup");
        METHODS_MAP.put(APICodes.ACTION_DELETE_STUDENTS, "students.deleteStudents");
        METHODS_MAP.put(APICodes.ACTION_GET_SUBJECTS, "subjects.getSubjects");
        METHODS_MAP.put(APICodes.ACTION_ADD_SUBJECT, "subjects.addSubject");
        METHODS_MAP.put(APICodes.ACTION_UPDATE_SUBJECT, "subjects.updateSubject");
        METHODS_MAP.put(APICodes.ACTION_DELETE_SUBJECTS, "subjects.deleteSubjects");
        METHODS_MAP.put(APICodes.ACTION_ADD_TEACHER, "teachers.addTeacher");
        METHODS_MAP.put(APICodes.ACTION_GET_TEACHERS, "teachers.getTeachers");
        METHODS_MAP.put(APICodes.ACTION_DELETE_TEACHERS, "teachers.deleteTeachers");
        METHODS_MAP.put(APICodes.ACTION_GET_TEACHER, "teachers.getTeacher");
        METHODS_MAP.put(APICodes.ACTION_GET_SUBJECTS_OF_TEACHER, "teachers.getSubjectsOfTeacher");
        METHODS_MAP.put(APICodes.ACTION_SET_SUBJECTS_OF_TEACHER, "teachers.setSubjectsOfTeacher");
        METHODS_MAP.put(APICodes.ACTION_UNSET_SUBJECTS_OF_TEACHER, "teachers.unsetSubjectsOfTeacher");
        METHODS_MAP.put(APICodes.ACTION_GET_GROUPS_OF_TEACHERS_SUBJECT, "subjects.getGroupsOfTeachersSubject");
        METHODS_MAP.put(APICodes.ACTION_SET_GROUPS_OF_TEACHERS_SUBJECT, "subjects.setGroupsOfTeachersSubject");
        METHODS_MAP.put(APICodes.ACTION_UNSET_GROUPS_OF_TEACHERS_SUBJECT, "subjects.unsetGroupsOfTeachersSubject");
        METHODS_MAP.put(APICodes.ACTION_GET_SUBJECTS_OF_ALL_TEACHERS, "teachers.getSubjectsOfAllTeachers");
        METHODS_MAP.put(APICodes.ACTION_GET_GROUPS_OF_ALL_TS, "subjects.getGroupsOfAllTeachersSubjects");
        METHODS_MAP.put(APICodes.ACTION_GET_WEEK_SCHEDULE_FOR_GROUP, "schedule.getWeekScheduleForGroup");
    }

    private WebApiServer(Context context, String host) {
        setHost(host);
        setContext(context);
        mClient = AndroidHttpClient.newInstance(System.getProperty("http.agent"));
    }

    public synchronized static WebApiServer instantiate(Context context, String host) {
        if (sServerInstance == null) {
            sServerInstance = new WebApiServer(context, host);
        } else {
            sServerInstance.setHost(host);
        }
        return sServerInstance;
    }

    public void setHost(String host) {
        mHost = host;
        mFullApiPath = mHost + "/api/index.php";
    }

    public void setContext(Context context) {
        mContext = context;
    }


    public void loadAvatar(JSONObject json) throws IOException {
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            String avatarUrl = "http://" + json.getString("avatar");
            in = (InputStream) new URL(avatarUrl).getContent();
            fos = mContext.openFileOutput(AVATAR_FILENAME, Context.MODE_PRIVATE);
            int b;
            while ((b = in.read()) != -1) {
                fos.write(b);
            }
        } catch (IOException e1) {
            ;
        } catch (JSONException e2) {
            ;
        } finally {
            if (in != null) {
                in.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    private String getHost(){
        String host = PreferenceManager.getDefaultSharedPreferences(mContext).getString(JournalApplication.PREFERENCE_KEY_HOST, "");
        return host;
    }

    private String getFullApiPath() {
        return getHost() + "/api/index.php";
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
        }
    }

    @Override
    public JSONObject getServerInfo(String host) throws Exception {
        HttpGet request = new HttpGet(String.format("%s/api/index.php?info.getInfo", host));
        return new JSONObject(doQuery(request));
    }

    @Override
    public JSONObject executeGetApiQuery(ApiAction action) throws Exception {
        if (!METHODS_MAP.containsKey(action.apiCode)) {
            throw new Exception("Unknown API method on client-side");
        }
        String method = METHODS_MAP.get(action.apiCode);
        String data = action.actionData.toString();
        String args = URLEncoder.encode(data, "UTF-8");
        String requestBody = (args.isEmpty()) ? method : String.format("%s=%s", method, args);
        HttpGet request = new HttpGet(String.format("%s?%s", getFullApiPath(), requestBody));
        String response = doQuery(request);
        return new JSONObject(response);
    }

    @Override
    public JSONObject executePostApiQuery(ApiAction action) throws Exception {
        if (!METHODS_MAP.containsKey(action.apiCode)) {
            throw new Exception("Unknown API method on client-side");
        }
        List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
        String method = METHODS_MAP.get(action.apiCode);
        JSONObject data = action.actionData;
        Iterator it = data.keys();
        while (it.hasNext()) {
            String key = (String) it.next();
            requestParams.add(new BasicNameValuePair(key, data.getString(key)));
        }
        HttpPost request = new HttpPost(getFullApiPath() + "?" + method);
        request.setEntity(new UrlEncodedFormEntity(requestParams, "UTF-8"));
        return new JSONObject(doQuery(request));
    }
}
