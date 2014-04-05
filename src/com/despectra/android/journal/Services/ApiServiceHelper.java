package com.despectra.android.journal.Services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.despectra.android.journal.Activities.ApiActivity;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Server.APICodes;

import java.util.*;

/**
 * Created by Dmitry on 25.03.14.
 */
public class ApiServiceHelper {
    private Context mAppContext;
    private ApiService mService;
    private ServiceConnection mServiceConnection;
    private ApiService.ApiServiceBinder mServiceBinder;
    private boolean mBound;
    private Map<String, RegisteredActivityHolder> mRegisteredActivities;

    private static ApiServiceHelper mInstance;

    private ApiServiceHelper() {
        mBound = false;
        mRegisteredActivities = new HashMap<String, RegisteredActivityHolder>(32);
    }

    public synchronized static ApiServiceHelper newInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ApiServiceHelper();
        }
        mInstance.setApplicationContext(context);
        return mInstance;
    }

    public void setApplicationContext(Context context) {
        mAppContext = context;
    }

    public void registerActivity(ApiActivity activity, Callback callback) {
        String activityName = activity.getClass().getSimpleName();
        //УБРАТЬ КАК МОЖНО СКОРЕЕ
        if (callback == null) {
            callback = (Callback) activity;
        }
        //ЭТОТ КОШМАР
        RegisteredActivityHolder holder = null;
        if (mRegisteredActivities.containsKey(activityName)) {
            holder = mRegisteredActivities.get(activityName);
            holder.setCallback(callback);
        } else {
            holder = new RegisteredActivityHolder(callback);
            mRegisteredActivities.put(activityName, holder);
        }
        activity.setServiceHelperController(new ActivityController(activityName));
        notifyCompletedActions(activityName);
    }

    public void unregisterActivity(ApiActivity activity) {
        String activityName = activity.getClass().getSimpleName();
        RegisteredActivityHolder holder = mRegisteredActivities.get(activityName);
        if (holder != null) {
            holder.setCallback(null);
        }
    }

    private void notifyCompletedActions(String activityName) {
        int activityState = ((JournalApplication) mAppContext).getActivityState(activityName);
        if (activityState == JournalApplication.ONRESUME) {
            RegisteredActivityHolder holder = mRegisteredActivities.get(activityName);
            Queue<ApiAction> completedActions = holder.completedActions;
            if (completedActions != null && completedActions.size() > 0) {
                Callback callback = holder.callback;
                if (callback != null) {
                    while (!completedActions.isEmpty()) {
                        ApiAction action = completedActions.poll();
                        callback.onResponse(action.apiCode, action.actionData);
                    }
                }
            }
        }
    }

    private void runAllPendingActions(String senderTag) {
        RegisteredActivityHolder holder = mRegisteredActivities.get(senderTag);
        if (holder.runningAction != null) {
            return;
        }
        mService.processApiAction(senderTag, holder.pendingActions.poll());
    }

    private void tryRunApiAction(String senderTag, ApiAction action) {
        RegisteredActivityHolder holder = mRegisteredActivities.get(senderTag);
        if (holder.runningAction != null || !mBound) {
            ApiAction actionBefore;
            if (holder.runningAction != null) {
                if (holder.pendingActions.isEmpty()) {
                    actionBefore = holder.runningAction;
                } else {
                    actionBefore = holder.pendingActions.peekLast();
                }
            } else if(!holder.pendingActions.isEmpty()) {
                actionBefore = holder.pendingActions.peekLast();
            } else {
                holder.pendingActions.offer(action);
                return;
            }
            if (!action.equals(actionBefore)) {
                holder.pendingActions.offer(action);
            }
        } else {
            holder.runningAction = action;
            mService.processApiAction(senderTag, action);
        }
    }

    private synchronized void runApiQuery(String senderTag, ApiAction action) {
        if (!mBound) {
            bindService(senderTag);
        }
        tryRunApiAction(senderTag, action);
    }

    private void runApiQuery(String senderTag, int apiCode, String... parameters) {
        runApiQuery(senderTag, new ApiAction(apiCode, parameters));
    }

    private void bindService(final String senderTag) {
        Intent intent = new Intent(mAppContext, ApiService.class);
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                mServiceBinder = ((ApiService.ApiServiceBinder)binder);
                mService = mServiceBinder.getService();
                mBound = true;
                runAllPendingActions(senderTag);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBound = false;
            }
        };
        mAppContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        if (mBound) {
            mAppContext.unbindService(mServiceConnection);
        }
    }

    public final void onServiceResponse(String senderTag, ApiAction action) {
        RegisteredActivityHolder holder = mRegisteredActivities.get(senderTag);
        holder.runningAction = null;
        Queue<ApiAction> completedActions = holder.completedActions;
        completedActions.offer(action);
        notifyCompletedActions(senderTag);
        Queue<ApiAction> pendingActions = holder.pendingActions;
        if (!pendingActions.isEmpty()) {
            runApiQuery(senderTag, pendingActions.poll());
        }
    }

    private class ActivityController implements Controller {
        private String mActivityName;

        public ActivityController(String activityName) {
            mActivityName = activityName;
        }

        public String getActivityName() {
            return mActivityName;
        }

        @Override
        public void login(String login, String passwd) {
            runApiQuery(mActivityName, APICodes.ACTION_LOGIN, login, passwd);
        }

        @Override
        public void logout(String token) {
            runApiQuery(mActivityName, APICodes.ACTION_LOGOUT, token);
        }

        @Override
        public void getApiInfo(String host) {
            runApiQuery(mActivityName, APICodes.ACTION_GET_INFO, host);
        }

        @Override
        public void getMinProfile(String token) {
            runApiQuery(mActivityName, APICodes.ACTION_GET_MIN_PROFILE, token);
        }

        @Override
        public void getEvents(String token, int offset, int count) {
            runApiQuery(mActivityName, APICodes.ACTION_GET_EVENTS, token, String.valueOf(offset), String.valueOf(count));
        }
    }

    private class RegisteredActivityHolder {
        public Deque<ApiAction> pendingActions;
        public ApiAction runningAction;
        public Deque<ApiAction> completedActions;
        public Callback callback;

        public RegisteredActivityHolder(Callback callback) {
            this.callback = callback;
            pendingActions = new LinkedList<ApiAction>();
            completedActions = new LinkedList<ApiAction>();
        }

        public void setCallback(Callback callback) {
            this.callback = callback;
        }
    }

    public static class ApiAction {
        public int apiCode;
        public Object actionData;
        public long creationTime;
        public ApiAction(int apiCode, Object actionData) {
            this.apiCode = apiCode;
            this.actionData = actionData;
            creationTime = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ApiAction)) {
                return false;
            }
            ApiAction action = (ApiAction) o;
            return apiCode == action.apiCode && actionData.equals(action.actionData);
        }
    }

    public interface Controller {
        public void login(String login, String passwd);
        public void logout(String token);
        public void getApiInfo(String host);
        public void getMinProfile(String token);
        public void getEvents(String token, int offset, int count);
    }

    public interface Callback {
        public void onResponse(int actionCode, Object response);
    }
}
