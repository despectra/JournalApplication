package com.despectra.android.journal.Services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Data.Contract;
import com.despectra.android.journal.Data.MainProvider;
import com.despectra.android.journal.Data.ProviderUpdater;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Server.ServerAPI;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by Dmitry on 25.03.14.
 */
public class ApiService extends Service {
    public static final String PACKAGE = JournalApplication.PACKAGE;

    public static final String ACTION_LOGIN = PACKAGE + ".LOGIN";
    public static final String ACTION_LOGOUT = PACKAGE + ".LOGOUT";
    public static final String ACTION_CHECK_TOKEN = PACKAGE + ".CHECK_TOKEN";
    public static final String ACTION_GET_PROFILE = PACKAGE + ".GET_PROFILE";
    public static final String ACTION_GET_AVATAR = PACKAGE + ".GET_AVATAR";
    public static final String ACTION_SET_SERVER_HOST = PACKAGE + ".SET_HOST";
    public static final String AVATAR_FILENAME = "user_avatar";
    private static final String TAG = "BACKGROUND_SERVICE";

    private static final ArrayDeque<String> API_ACTIONS = new ArrayDeque<String>(Arrays.asList(
            ACTION_LOGIN,
            ACTION_LOGOUT,
            ACTION_CHECK_TOKEN,
            ACTION_GET_PROFILE,
            ACTION_GET_AVATAR));

    private static ServerAPI mServer;
    private ApiServiceBinder mBinder;
    private ApiServiceHelper mServiceHelper;
    private ProviderUpdater mUpdater;
    private Handler mResponseHandler;

    public ApiService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, String.format("Service OnCreate: %d", hashCode()));
        mServer = ServerAPI.instantiate(getApplicationContext(), getHostFromPreferences());
        mBinder = new ApiServiceBinder();
        JournalApplication application = (JournalApplication)getApplicationContext();
        mServiceHelper = application.getApiServiceHelper();
        mResponseHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Response response = (Response) msg.obj;
                mServiceHelper.onServiceResponse(response.senderTag, response.action);
            }
        };
        mUpdater = new ProviderUpdater(getApplicationContext(), Contract.STRING_URI);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, String.format("Service OnDestroy: %d", hashCode()));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void processApiAction(final String senderTag, final ApiServiceHelper.ApiAction action) {
        processApiAction(senderTag, action.apiCode, (String[])action.actionData);
    }

    public void processApiAction(final String senderTag, final int apiActionCode, final String... parameters) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Response response = new Response();
                try {
                    JSONObject data = new JSONObject();
                    response.senderTag = senderTag;
                    for (int i = 0; i < parameters.length; i++) {
                        parameters[i] = parameters[i].replaceAll("\"", "\\\"");
                    }
                    switch (apiActionCode) {
                        case APICodes.ACTION_LOGIN:
                            data = mServer.login(parameters[0], parameters[1]);
                            break;
                        case APICodes.ACTION_LOGOUT:
                            data = mServer.logout(parameters[0]);
                            break;
                        case APICodes.ACTION_GET_MIN_PROFILE:
                            data = mServer.getMinProfile(parameters[0]);
                            break;
                        case APICodes.ACTION_CHECK_TOKEN:
                            data = mServer.checkToken(parameters[0]);
                            break;
                        case APICodes.ACTION_GET_INFO:
                            data = mServer.getApiInfo(parameters[0]);
                            break;
                        case APICodes.ACTION_GET_EVENTS:
                            data = mServer.getEvents(parameters[0], String.valueOf(parameters[1]), String.valueOf(parameters[2]));
                            if (data.has("events")) {
                                mUpdater.updateTableWithJSONArray(
                                            ProviderUpdater.MODE_APPEND,
                                            Contract.Events.TABLE,
                                            data.getJSONArray("events"),
                                            new String[]{"id", "text", "datetime"},
                                            new String[]{"_id", "text", "datetime"},
                                            "id",
                                            "_id"
                                        );
                            }
                            break;
                        case APICodes.ACTION_ADD_GROUP:
                            ContentValues group = new ContentValues();
                            group.put("name", parameters[1]);
                            if (parameters[2] == null) {
                                parameters[2] = "0";
                            }
                            group.put("parent_id", parameters[2]);
                            long localId = mUpdater.insertTempRow("groups", group);
                            //########   FOR DEBUG ########################
                            SystemClock.sleep(30000);
                            //##############################################
                            data = mServer.addGroup(parameters[0], parameters[1], parameters[2]);
                            if (data.has("group_id")) {
                                mUpdater.persistTempRow("groups", localId, data.getLong("group_id"));
                            }
                            break;
                        case APICodes.ACTION_GET_GROUPS:
                            data = mServer.getGroups(parameters[0], parameters[1], parameters[2], parameters[3]);
                            if (data.has("groups")) {
                                mUpdater.updateTableWithJSONArray(
                                        ProviderUpdater.MODE_REPLACE,
                                        Contract.Groups.TABLE,
                                        data.getJSONArray("groups"),
                                        new String[]{"id", "name", "parent_id"},
                                        new String[]{"remote_id", "name", "parent_id"},
                                        "id",
                                        "remote_id"
                                );
                            }
                            break;
                    }
                    response.action = new ApiServiceHelper.ApiAction(apiActionCode, data);
                    mResponseHandler.sendMessage(Message.obtain(mResponseHandler, apiActionCode, response));
                } catch (Exception ex) {
                    response.action = new ApiServiceHelper.ApiAction(-1, ex.getMessage());
                    mResponseHandler.sendMessage(Message.obtain(mResponseHandler, -1, response));
                }
            }
        }).start();
    }

    private String getHostFromPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return preferences.getString(JournalApplication.PREFERENCE_KEY_HOST, "");
    }

    private static class Response {
        String senderTag;
        ApiServiceHelper.ApiAction action;
    }

    class ApiServiceBinder extends Binder {
        ApiService getService() {
            return ApiService.this;
        }
    }

}
