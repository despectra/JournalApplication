package com.despectra.android.journal.App;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.despectra.android.journal.Services.ApiServiceHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmitry on 25.03.14.
 */
public class JournalApplication extends Application {

    public static String PACKAGE;
    public static final String TAG = "JOURNAL_APPLICATION";
    public static final String PREFERENCE_KEY_HOST = "settings_host";

    public static final String PREFERENCE_KEY_TOKEN = "token";
    public static final String PREFERENCE_KEY_UID = "uid";
    public static final String PREFERENCE_KEY_NAME = "name";
    public static final String PREFERENCE_KEY_MIDDLENAME = "middlename";
    public static final String PREFERENCE_KEY_SURNAME = "surname";
    public static final String PREFERENCE_KEY_LEVEL = "level";

    public static final int ONCREATE = 0;
    public static final int ONSTART = 1;
    public static final int ONRESUME = 2;
    public static final int ONPAUSE = 3;
    public static final int ONSTOP = 4;
    public static final int ONDESTROY = 5;

    private Map<String, Integer> mActivitiesStates;

    @Override
    public void onCreate() {
        super.onCreate();
        PACKAGE = getApplicationContext().getPackageName();
    }

    public void activityStateChanged(String activityName, int state) {
        if (mActivitiesStates == null) {
            mActivitiesStates = new HashMap<String, Integer>(32);
        }
        mActivitiesStates.put(activityName, state);
        //
        for (Map.Entry<String, Integer> curState : mActivitiesStates.entrySet()) {
            Log.v(TAG, curState.toString());
        }
        //
    }

    public int getActivityState(String activityName) {
        return mActivitiesStates.containsKey(activityName) ? mActivitiesStates.get(activityName) : -1;
    }

    public ApiServiceHelper getApiServiceHelper() {
        return ApiServiceHelper.newInstance(this);
    }
}