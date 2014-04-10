package com.despectra.android.journal.Services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.despectra.android.journal.Activities.AbstractApiActivity;
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
    private Map<String, RegisteredClientHolder> mRegisteredClients;

    private static ApiServiceHelper mInstance;

    private ApiServiceHelper() {
        mBound = false;
        mRegisteredClients = new HashMap<String, RegisteredClientHolder>(32);
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

    public void registerClient(ApiClient client, Callback callback) {
        String name = client.getClientName();
        RegisteredClientHolder holder;
        if (mRegisteredClients.containsKey(name)) {
            holder = mRegisteredClients.get(name);
            holder.setCallback(callback);
        } else {
            holder = new RegisteredClientHolder(callback);
            mRegisteredClients.put(name, holder);
        }
        client.setServiceHelperController(new BaseClientController(name));
        notifyCompletedActions(name);
    }

    public void unregisterClient(ApiClient client) {
        String activityName = client.getClientName();
        RegisteredClientHolder holder = mRegisteredClients.get(activityName);
        if (holder != null) {
            holder.setCallback(null);
        }
    }

    private void notifyCompletedActions(String clientName) {
        int activityState = ((JournalApplication) mAppContext).getActivityState(clientName);
        if (activityState == JournalApplication.ONRESUME) {
            RegisteredClientHolder holder = mRegisteredClients.get(clientName);
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

    private int hasRunningActionForClient(String clientName) {
        RegisteredClientHolder holder = mRegisteredClients.get(clientName);
        if (holder.runningAction != null) {
            return holder.runningAction.apiCode;
        } else {
            return -1;
        }
    }

    private void runAllPendingActions(String senderTag) {
        RegisteredClientHolder holder = mRegisteredClients.get(senderTag);
        if (holder.runningAction != null) {
            return;
        }
        ApiAction startingAction = holder.pendingActions.poll();
        holder.runningAction = startingAction;
        mService.processApiAction(senderTag, startingAction);
    }

    private void tryRunApiAction(String senderTag, ApiAction action) {
        RegisteredClientHolder holder = mRegisteredClients.get(senderTag);
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
        RegisteredClientHolder holder = mRegisteredClients.get(senderTag);
        holder.runningAction = null;
        Queue<ApiAction> completedActions = holder.completedActions;
        completedActions.offer(action);
        notifyCompletedActions(senderTag);
        Queue<ApiAction> pendingActions = holder.pendingActions;
        if (!pendingActions.isEmpty()) {
            runApiQuery(senderTag, pendingActions.poll());
        }
    }

    private class BaseClientController implements Controller {
        private String mClientName;

        public BaseClientController(String activityName) {
            mClientName = activityName;
        }

        @Override
        public int hasRunningAction() {
            return hasRunningActionForClient(mClientName);
        }

        @Override
        public void login(String login, String passwd) {
            runApiQuery(mClientName, APICodes.ACTION_LOGIN, login, passwd);
        }

        @Override
        public void logout(String token) {
            runApiQuery(mClientName, APICodes.ACTION_LOGOUT, token);
        }

        @Override
        public void getApiInfo(String host) {
            runApiQuery(mClientName, APICodes.ACTION_GET_INFO, host);
        }

        @Override
        public void getMinProfile(String token) {
            runApiQuery(mClientName, APICodes.ACTION_GET_MIN_PROFILE, token);
        }

        @Override
        public void getEvents(String token, int offset, int count) {
            runApiQuery(mClientName, APICodes.ACTION_GET_EVENTS, token, String.valueOf(offset), String.valueOf(count));
        }

        @Override
        public void getAllEvents(String token) {
            getEvents(token, 0, 0);
        }

        @Override
        public void addGroup(String token, String name, long parentId) {
            runApiQuery(mClientName, APICodes.ACTION_ADD_GROUP, token, name, String.valueOf(parentId));
        }

        @Override
        public void getAllGroups(String token, long parentGroupId) {
            getGroups(token, parentGroupId, 0, 0);
        }

        @Override
        public void getGroups(String token, long parentGroupId, int offset, int count) {
            runApiQuery(mClientName, APICodes.ACTION_GET_GROUPS,
                    token,
                    String.valueOf(parentGroupId),
                    String.valueOf(offset),
                    String.valueOf(count));
        }
    }

    private class RegisteredClientHolder {
        public Deque<ApiAction> pendingActions;
        public ApiAction runningAction;
        public Deque<ApiAction> completedActions;
        public Callback callback;

        public RegisteredClientHolder(Callback callback) {
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
        public int hasRunningAction();

        public void login(String login, String passwd);
        public void logout(String token);
        public void getApiInfo(String host);
        public void getMinProfile(String token);
        public void getEvents(String token, int offset, int count);
        public void getAllEvents(String token);
        public void addGroup(String token, String name, long parentId);
        public void getAllGroups(String token, long parentGroupId);
        public void getGroups(String token, long parentGroupId, int offset, int count);
    }

    public interface ApiClient {
        public void setServiceHelperController(Controller controller);
        public String getClientName();
    }

    public interface Callback {
        public void onResponse(int actionCode, Object response);
    }
}
