package com.despectra.android.journal.logic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.logic.services.ApiService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by Dmitry on 25.03.14.
 */
public class ApiServiceHelper {
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_HIGH = 1;

    private Context mAppContext;
    private ApiService mService;
    private ServiceConnection mServiceConnection;
    private ApiService.ApiServiceBinder mServiceBinder;
    private boolean mBound;
    private Map<String, RegisteredClient> mRegisteredClients;

    private static ApiServiceHelper sInstance;

    private ApiServiceHelper() {
        mBound = false;
        mRegisteredClients = new HashMap<String, RegisteredClient>(32);
    }

    public synchronized static ApiServiceHelper newInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ApiServiceHelper();
        }
        sInstance.setApplicationContext(context);
        return sInstance;
    }

    public void setApplicationContext(Context context) {
        mAppContext = context;
    }

    public void registerClient(ApiClient client, Callback callback) {
        String name = client.getClientName();
        RegisteredClient holder;
        if (mRegisteredClients.containsKey(name)) {
            holder = mRegisteredClients.get(name);
            holder.setCallback(callback);
        } else {
            holder = new RegisteredClient(callback);
            mRegisteredClients.put(name, holder);
        }
        client.setServiceHelperController(new BaseClientController(name));
        notifyCompletedActions(name);
    }

    public void unregisterClient(ApiClient client) {
        String activityName = client.getClientName();
        RegisteredClient holder = mRegisteredClients.get(activityName);
        if (holder != null) {
            holder.setCallback(null);
        }
    }

    private void notifyCompletedActions(String clientName) {
        int activityState = ((JournalApplication) mAppContext).getActivityState(clientName);
        if (activityState == JournalApplication.ONRESUME) {
            RegisteredClient holder = mRegisteredClients.get(clientName);
            Queue<ApiAction> completedActions = holder.completedActions;
            if (completedActions != null && completedActions.size() > 0) {
                Callback callback = holder.callback;
                if (callback != null) {
                    while (!completedActions.isEmpty()) {
                        ApiAction action = completedActions.poll();
                        int remaining = holder.pendingActions.size() + holder.runningActions.size();
                        callback.onResponse(action.apiCode, remaining, action.actionData);
                    }
                }
            }
        }
    }

    private void tryRunApiAction(String senderTag, ApiAction action, int priority) {
        RegisteredClient holder = mRegisteredClients.get(senderTag);
        int pendingCount = holder.pendingActions.size();
        int runningCount = holder.runningActions.size();

        ApiAction actionBefore;
        if (priority == PRIORITY_HIGH) {
            if (runningCount > 0) {
                actionBefore = getLastActionByTime(holder.runningActions.iterator());
                if (!action.equals(actionBefore)) {
                    runHighPriorityAction(senderTag, action, holder);
                }
            } else {
                if (pendingCount > 0) {
                    actionBefore = holder.pendingActions.peekLast();
                    if (!action.equals(actionBefore)) {
                        runHighPriorityAction(senderTag, action, holder);
                    }
                } else {
                    runHighPriorityAction(senderTag, action, holder);
                }
            }
        } else {
            if (pendingCount > 0) {
                actionBefore = holder.pendingActions.peekLast();
                if (!action.equals(actionBefore)) {
                    holder.pendingActions.offer(action);
                }
            } else {
                if (runningCount >= 1) {
                    actionBefore = getLastActionByTime(holder.runningActions.iterator());
                    if (!action.equals(actionBefore)) {
                        holder.pendingActions.offer(action);
                    }
                } else {
                    if (mBound) {
                        holder.runningActions.add(action);
                        mService.processApiAction(action);
                    } else {
                        holder.pendingActions.offer(action);
                    }
                }
            }
        }
    }

    private void runHighPriorityAction(String senderTag, ApiAction action, RegisteredClient holder) {
        holder.runningActions.add(action);
        if (mBound) {
            mService.processApiAction(action);
        }
    }

    private ApiAction getLastActionByTime(Iterator<ApiAction> actionsIt) {
        long newestTime = 0;
        ApiAction newest = null;
        while (actionsIt.hasNext()) {
            ApiAction action = actionsIt.next();
            if (action.creationTime > newestTime) {
                newestTime = action.creationTime;
                newest = action;
            }
        }
        return newest;
    }

    private synchronized void startApiQuery(String senderTag, ApiAction action, int priority) {
        if (!mBound) {
            bindService(senderTag);
        }
        tryRunApiAction(senderTag, action, priority);
    }

    private void bindService(final String senderTag) {
        Intent intent = new Intent(mAppContext, ApiService.class);
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                mServiceBinder = ((ApiService.ApiServiceBinder)binder);
                mService = mServiceBinder.getService();
                mBound = true;
                launchApiQueryingProcess(senderTag);
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

    private void launchApiQueryingProcess(String senderTag) {
        RegisteredClient holder = mRegisteredClients.get(senderTag);
        if (!holder.runningActions.isEmpty()) {
            for (ApiAction action : holder.runningActions) {
                mService.processApiAction(action);
            }
            return;
        }
        ApiAction action = holder.pendingActions.poll();
        holder.runningActions.add(action);
        mService.processApiAction(action);
    }

    public final synchronized void onServiceProgress(String clientName, Object data) {
        int activityState = ((JournalApplication) mAppContext).getActivityState(clientName);
        if (activityState == JournalApplication.ONRESUME) {
            RegisteredClient holder = mRegisteredClients.get(clientName);
            if (holder.callback != null && holder.callback instanceof FeedbackApiClient) {
                ((FeedbackApiClient) holder.callback).onProgress(data);
            }
        }
    }

    public final synchronized void onServiceResponse(ApiService.Response response) {
        String senderTag = response.responseAction.clientTag;
        ApiAction initialAction = response.initialAction;
        ApiAction responseAction = response.responseAction;

        RegisteredClient holder = mRegisteredClients.get(senderTag);
        holder.runningActions.remove(initialAction);
        holder.completedActions.offer(responseAction);
        notifyCompletedActions(senderTag);

        if (!holder.pendingActions.isEmpty()) {
            startApiQuery(senderTag, holder.pendingActions.poll(), PRIORITY_HIGH);
        }
    }

    private class BaseClientController implements Controller {
        private String mClientName;

        public BaseClientController(String activityName) {
            mClientName = activityName;
        }

        @Override
        public int getRunningActionsCount() {
            RegisteredClient holder = mRegisteredClients.get(mClientName);
            return holder.runningActions.size();
        }

        @Override
        public int getLastRunningActionCode() {
            if (getRunningActionsCount() == 1) {
                return mRegisteredClients.get(mClientName).runningActions.get(0).apiCode;
            }
            return -1;
        }

        @Override
        public void login(String login, String passwd, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("login", login);
                data.put("passwd", passwd);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_LOGIN, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void logout(String token, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_LOGOUT, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void getApiInfo(String host, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("host", host);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_INFO, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void getMinProfile(String token, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_MIN_PROFILE, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void getEvents(String token, int offset, int count, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                data.put("count", count);
                data.put("offset", offset);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_EVENTS, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void getAllEvents(String token, int priority) {
            getEvents(token, 0, 0, priority);
        }

        @Override
        public void addGroup(String token, String name, long localParentId, long remoteParentId, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                data.put("name", name);
                data.put("LOCAL_parent_id", localParentId);
                data.put("parent_id", remoteParentId);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_ADD_GROUP, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void getAllGroups(String token, long localParentId, long remoteParentId, int priority) {
            getGroups(token, localParentId, remoteParentId, 0, 0, priority);
        }

        @Override
        public void getGroups(String token, long localParentId, long remoteParentId, int offset, int count, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                data.put("LOCAL_parent_group_id", localParentId);
                data.put("parent_group_id", remoteParentId);
                data.put("offset", offset);
                data.put("count", count);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_GROUPS, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void deleteGroups(String token, long[] localIds, long[] remoteIds, int priority) {
            JSONObject data = new JSONObject();
            try {
                JSONArray localGroups = new JSONArray();
                JSONArray remoteGroups = new JSONArray();
                for (int i = 0; i < localIds.length; i++) {
                    localGroups.put(localIds[i]);
                    remoteGroups.put(remoteIds[i]);
                }
                data.put("token", token);
                data.put("LOCAL_groups", localGroups);
                data.put("groups", remoteGroups);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_DELETE_GROUPS, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void updateGroup(String token, long localId, long remoteId, String updName,
                                long updLocalParentId, long updRemoteParentId, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                data.put("LOCAL_id", localId);
                data.put("id", remoteId);
                JSONObject groupData = new JSONObject();
                groupData.put("name", updName);
                groupData.put("LOCAL_parent_id", updLocalParentId);
                groupData.put("parent_id", updRemoteParentId);
                data.put("data", groupData);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_UPDATE_GROUP, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void getStudentsByGroup(String token, long localGroupId, long remoteGroupId, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                data.put("LOCAL_group_id", localGroupId);
                data.put("group_id", remoteGroupId);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_STUDENTS_BY_GROUP, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void addStudentIntoGroup(String token, long localGroupId, long remoteGroupId, String name, String middlename, String surname, String login, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                data.put("LOCAL_group_id", localGroupId);
                data.put("group_id", remoteGroupId);
                data.put("name", name);
                data.put("middlename", middlename);
                data.put("surname", surname);
                data.put("login", login);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_ADD_STUDENT_IN_GROUP, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void deleteStudents(String token, long[] localIds, long[] remoteIds, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                JSONArray localStudents = new JSONArray();
                JSONArray remoteStudents = new JSONArray();
                for (int i = 0; i < localIds.length; i++) {
                    localStudents.put(localIds[i]);
                    remoteStudents.put(remoteIds[i]);
                }
                data.put("LOCAL_students", localStudents);
                data.put("students", remoteStudents);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_DELETE_STUDENTS, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void getSubjects(String token, int priority) {
            JSONObject data = new JSONObject();
            try {
                data.put("token", token);
                startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_SUBJECTS, mClientName, data), priority);
            } catch (JSONException e) {
            }
        }

        @Override
        public void addMockMarks(long groupId) {
            if (mBound) {
                try {
                    mService.processApiAction(new ApiAction(100500, mClientName, new JSONObject(String.format("{\"group\":\"%d\"}", groupId))));
                } catch (JSONException e) {

                }
            }
        }

        @Override
        public void updateMockMark(long markId, int mark) {
            if (mBound) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("markId", markId);
                    json.put("mark", mark);
                    mService.processApiAction(new ApiAction(100599, mClientName, json));
                } catch (JSONException e) {

                }
            }
        }


    }

    private class RegisteredClient {
        public Deque<ApiAction> pendingActions;
        public List<ApiAction> runningActions;
        public Deque<ApiAction> completedActions;
        public Callback callback;

        public RegisteredClient(Callback callback) {
            this.callback = callback;
            pendingActions = new LinkedList<ApiAction>();
            runningActions = new ArrayList<ApiAction>();
            completedActions = new LinkedList<ApiAction>();
        }

        public void setCallback(Callback callback) {
            this.callback = callback;
        }
    }

    public static class ApiAction {
        public int apiCode;
        public JSONObject actionData;
        public long creationTime;
        public String clientTag;
        public ApiAction(int apiCode, String senderTag, JSONObject actionData) {
            this.apiCode = apiCode;
            this.actionData = actionData;
            this.clientTag = senderTag;
            creationTime = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ApiAction)) {
                return false;
            }
            ApiAction action = (ApiAction) o;
            return apiCode == action.apiCode && clientTag.equals(action.clientTag) && actionData.equals(action.actionData);
        }
    }

    public interface Controller {
        public int getRunningActionsCount();
        public int getLastRunningActionCode();

        public void login(String login, String passwd, int priority);
        public void logout(String token, int priority);
        public void getApiInfo(String host, int priority);
        public void getMinProfile(String token, int priority);
        public void getEvents(String token, int offset, int count, int priority);
        public void getAllEvents(String token, int priority);
        public void addGroup(String token, String name, long localParentId, long remoteParentId, int priority);
        public void getAllGroups(String token, long localParentId, long remoteParentId, int priority);
        public void getGroups(String token, long localParentId, long remoteParentId, int offset, int count, int priority);
        public void deleteGroups(String token, long[] localIds, long[] remoteIds, int priority);
        public void updateGroup(String token, long localId, long remoteId, String updName, long updLocalParentId, long updRemoteParentId, int priority);
        public void getStudentsByGroup(String token, long localGroupId, long remoteGroupId, int priority);
        public void addStudentIntoGroup(String token, long localGroupId, long remoteGroupId, String name, String middlename, String surname, String login, int priority);
        public void deleteStudents(String token, long[] localIds, long[] remoteIds, int priority);
        public void getSubjects(String token, int priority);

        // TEMPORARY
        public void addMockMarks(long groupId);
        public void updateMockMark(long markId, int mark);
    }

    public interface FeedbackApiClient extends ApiClient {
        public void onProgress(Object data);
    }

    public interface ApiClient extends Callback {
        public void setServiceHelperController(Controller controller);
        public String getClientName();
    }

    public interface Callback {
        public void onResponse(int actionCode, int remainingActions, Object response);
    }
}
