package com.despectra.android.journal.logic.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.services.ApiService;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.utils.JSONBuilder;
import com.despectra.android.journal.utils.Utils;
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
    private Map<String, RegisteredClient> mRegisteredClients;
    private boolean mBound;

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
        client.setServiceHelperController(new BaseClientHelperController(name));
        notifyCompletedActions(name);
    }

    public void unregisterClient(com.despectra.android.journal.logic.helper.ApiClient client) {
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
                    runHighPriorityAction(action, holder);
                }
            } else {
                if (pendingCount > 0) {
                    actionBefore = holder.pendingActions.peekLast();
                    if (!action.equals(actionBefore)) {
                        runHighPriorityAction(action, holder);
                    }
                } else {
                    runHighPriorityAction(action, holder);
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
                        tryShowProgress(holder);
                        mService.processApiAction(action);
                    } else {
                        holder.pendingActions.offer(action);
                    }
                }
            }
        }
    }

    private void runHighPriorityAction(ApiAction action, RegisteredClient holder) {
        holder.runningActions.add(action);
        if (mBound) {
            tryShowProgress(holder);
            mService.processApiAction(action);
        }
    }

    private void tryShowProgress(RegisteredClient client) {
        if(client.callback instanceof ApiClientWithProgress) {
            ((ApiClientWithProgress)client.callback).showProgress();
        }
    }

    private void tryHideProgress(RegisteredClient client) {
        if(client.callback instanceof ApiClientWithProgress) {
            ((ApiClientWithProgress)client.callback).hideProgress();
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
            tryShowProgress(holder);
            for (ApiAction action : holder.runningActions) {
                mService.processApiAction(action);
            }
            return;
        }
        ApiAction action = holder.pendingActions.poll();
        holder.runningActions.add(action);
        tryShowProgress(holder);
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
        } else {
            tryHideProgress(holder);
        }
    }

    private class BaseClientHelperController implements HelperController {
        private String mClientName;

        public BaseClientHelperController(String activityName) {
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
            JSONObject data = new JSONBuilder()
                    .addKeyValue("login", login)
                    .addKeyValue("passwd", Utils.md5(passwd)).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_LOGIN, mClientName, data), priority);
        }

        @Override
        public void logout(String token, int priority) {
            JSONObject data = new JSONBuilder().addKeyValue("token", token).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_LOGOUT, mClientName, data), priority);
        }

        @Override
        public void checkToken(String token, int priority) {
            JSONObject data = new JSONBuilder().addKeyValue("token", token).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_CHECK_TOKEN, mClientName, data), priority);
        }

        @Override
        public void getApiInfo(String host, int priority) {
            JSONObject data = new JSONBuilder().addKeyValue("host", host).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_INFO, mClientName, data), priority);
        }

        @Override
        public void getMinProfile(String token, int priority) {
            JSONObject data = new JSONBuilder().addKeyValue("token", token).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_MIN_PROFILE, mClientName, data), priority);
        }

        @Override
        public void getEvents(String token, int offset, int count, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addKeyValue("count", count)
                    .addKeyValue("offset", offset).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_EVENTS, mClientName, data), priority);
        }

        @Override
        public void getAllEvents(String token, int priority) {
            getEvents(token, 0, 0, priority);
        }

        @Override
        public void addGroup(String token, String name, EntityIds parentIds, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addKeyValue("name", name)
                .addKeyValue("LOCAL_parent_id", parentIds.getLocalId())
                .addKeyValue("parent_id", parentIds.getRemoteId()).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_ADD_GROUP, mClientName, data), priority);
        }

        @Override
        public void getAllGroups(String token, EntityIds parentIds, int priority) {
            getGroups(token, parentIds, 0, 0, priority);
        }

        @Override
        public void getGroups(String token, EntityIds parentIds, int offset, int count, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("parent_group_id", parentIds)
                .addKeyValue("offset", offset)
                .addKeyValue("count", count).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_GROUPS, mClientName, data), priority);
        }

        @Override
        public void deleteGroups(String token, EntityIds[] ids, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIdsArray("groups", ids).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_DELETE_GROUPS, mClientName, data), priority);
        }

        @Override
        public void updateGroup(String token, EntityIds ids, String updName,
                                EntityIds parentIds, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("id", ids)
                .addKeyValue("data", new JSONBuilder()
                        .addKeyValue("name", updName)
                        .addEntityIds("parent_id", parentIds)
                        .create())
                .create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_UPDATE_GROUP, mClientName, data), priority);
        }

        @Override
        public void getStudentsByGroup(String token, EntityIds groupIds, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("group_id", groupIds)
                .create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_STUDENTS_BY_GROUP, mClientName, data), priority);
        }

        @Override
        public void addStudentIntoGroup(String token, EntityIds groupIds, String name, String middlename, String surname, String login, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("group_id", groupIds)
                .addKeyValue("name", name)
                .addKeyValue("middlename", middlename)
                .addKeyValue("surname", surname)
                .addKeyValue("login", login).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_ADD_STUDENT_IN_GROUP, mClientName, data), priority);
        }

        @Override
        public void deleteStudents(String token, EntityIds[] ids, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIdsArray("students", ids)
                    .create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_DELETE_STUDENTS, mClientName, data), priority);
        }

        @Override
        public void getSubjects(String token, int offset, int count, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addKeyValue("offset", offset)
                .addKeyValue("count", count).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_SUBJECTS, mClientName, data), priority);
        }

        @Override
        public void getAllSubjects(String token, int priority) {
            getSubjects(token, 0, 0, priority);
        }

        @Override
        public void addSubject(String token, String name, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addKeyValue("name", name).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_ADD_SUBJECT, mClientName, data), priority);
        }

        @Override
        public void updateSubject(String token, EntityIds ids, String updName, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("id", ids)
                .addKeyValue("data", new JSONBuilder()
                        .addKeyValue("name", updName).create()).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_UPDATE_SUBJECT, mClientName, data), priority);
        }

        @Override
        public void deleteSubjects(String token, EntityIds[] ids, int priority) {
            JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIdsArray("subjects", ids)
                .create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_DELETE_SUBJECTS, mClientName, data), priority);
        }

        @Override
        public void addTeacher(String token, String firstName, String middleName, String secondName, String login, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addKeyValue("firstName", firstName)
                    .addKeyValue("middleName", middleName)
                    .addKeyValue("secondName", secondName)
                    .addKeyValue("login", login).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_ADD_TEACHER, mClientName, data), priority);
        }

        @Override
        public void getTeachers(String token, int offset, int count, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addKeyValue("offset", offset)
                    .addKeyValue("count", count).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_TEACHERS, mClientName, data), priority);
        }

        @Override
        public void deleteTeachers(String token, EntityIds[] ids, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIdsArray("teachers", ids)
                    .create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_DELETE_TEACHERS, mClientName, data), priority);
        }

        @Override
        public void getTeacher(String token, EntityIds userIds, EntityIds teacherIds, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addKeyValue("user_id", userIds.getLocalId())
                    .addEntityIds("teacher_id", teacherIds)
                    .create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_TEACHER, mClientName, data), priority);
        }

        @Override
        public void getSubjectsOfTeacher(String token, EntityIds teacherIds, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIds("teacher_id", teacherIds)
                    .create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_SUBJECTS_OF_TEACHER, mClientName, data), priority);
        }

        @Override
        public void setSubjectsOfTeacher(String token, EntityIds teacherIds, EntityIds[] subjectsIds, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIds("teacher_id", teacherIds)
                    .addEntityIdsArray("subjects_ids", subjectsIds).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_SET_SUBJECTS_OF_TEACHER, mClientName, data), priority);
        }

        @Override
        public void unsetSubjectsOfTeacher(String token, EntityIds[] linksIds, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIdsArray("links_ids", linksIds).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_UNSET_SUBJECTS_OF_TEACHER, mClientName, data), priority);
        }

        @Override
        public void getGroupsOfTeachersSubject(String token, EntityIds teacherSubjectIds, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIds("teacher_subject_id", teacherSubjectIds)
                    .create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_GET_GROUPS_OF_TEACHERS_SUBJECT, mClientName, data), priority);
        }

        @Override
        public void setGroupsOfTeachersSubject(String token, EntityIds teacherSubjectIds, EntityIds[] groupsIds, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIds("teacher_subject_id", teacherSubjectIds)
                    .addEntityIdsArray("groups_ids", groupsIds).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_SET_GROUPS_OF_TEACHERS_SUBJECT, mClientName, data), priority);
        }

        @Override
        public void unsetGroupsOfTeachersSubject(String token, EntityIds[] linksIds, int priority) {
            JSONObject data = new JSONBuilder()
                    .addKeyValue("token", token)
                    .addEntityIdsArray("links_ids", linksIds).create();
            startApiQuery(mClientName, new ApiAction(APICodes.ACTION_UNSET_GROUPS_OF_TEACHERS_SUBJECT, mClientName, data), priority);
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

    public interface Callback {
        public void onResponse(int actionCode, int remainingActions, Object response);
    }
}
