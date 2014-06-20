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

    public void registerClient(ApiClient client, HelperController controller) {
        String name = client.getClientName();
        RegisteredClient holder;
        if (mRegisteredClients.containsKey(name)) {
            holder = mRegisteredClients.get(name);
            holder.setClient(client);
        } else {
            holder = new RegisteredClient(client);
            mRegisteredClients.put(name, holder);
        }
        client.setServiceHelperController(controller);
        notifyCompletedActions(name);
    }

    public void unregisterClient(ApiClient client) {
        String clientName = client.getClientName();
        RegisteredClient holder = mRegisteredClients.get(clientName);
        if (holder != null) {
            holder.setClient(null);
        }
    }

    private void notifyCompletedActions(String clientName) {
        int activityState = ((JournalApplication) mAppContext).getActivityState(clientName);
        if (activityState == JournalApplication.ONRESUME) {
            RegisteredClient clientHolder = mRegisteredClients.get(clientName);
            Queue<ApiAction> completedActions = clientHolder.completedActions;
            if (completedActions != null && completedActions.size() > 0) {
                ApiClient client = clientHolder.client;
                if (client != null) {
                    while (!completedActions.isEmpty()) {
                        ApiAction action = completedActions.poll();
                        int remaining = clientHolder.pendingActions.size() + clientHolder.runningActions.size();
                        client.onResponse(action.apiCode, remaining, action.actionData);
                    }
                }
            }
        }
    }

    private void tryRunApiAction(String senderTag, ApiAction action, int priority) {
        RegisteredClient holder = mRegisteredClients.get(senderTag);
        int pendingCount = holder.pendingActions.size();
        int runningCount = holder.runningActions.size();

        if (priority == PRIORITY_HIGH) {
            if (runningCount > 0) {
                if(isActiongRepeatingInCollection(action, holder.runningActions)) {
                    return;
                }
            }
            if (pendingCount > 0) {
                if(isActiongRepeatingInCollection(action, holder.pendingActions)) {
                    return;
                }
            }
            runHighPriorityAction(action, holder);
            /*if (runningCount > 0) {
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
            }*/
        } else {
            if (pendingCount > 0) {
                if (isActiongRepeatingInCollection(action, holder.pendingActions)) {
                    return;
                }
                holder.pendingActions.offer(action);
            } else {
                if (runningCount >= 1) {
                    if (isActiongRepeatingInCollection(action, holder.runningActions)) {
                        return;
                    }
                    holder.pendingActions.offer(action);
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

    private boolean isActiongRepeatingInCollection(ApiAction action, Iterable<ApiAction> collection) {
        for (ApiAction oneAction : collection) {
            if (action.hash.equals(oneAction.hash)) {
                return true;
            }
        }
        return false;
    }

    private void runHighPriorityAction(ApiAction action, RegisteredClient holder) {
        holder.runningActions.add(action);
        if (mBound) {
            tryShowProgress(holder);
            mService.processApiAction(action);
        }
    }

    private void tryShowProgress(RegisteredClient client) {
        if(client.client instanceof ApiClientWithProgress) {
            ((ApiClientWithProgress)client.client).showProgress();
        }
    }

    private void tryHideProgress(RegisteredClient client) {
        if(client.client instanceof ApiClientWithProgress) {
            ((ApiClientWithProgress)client.client).hideProgress();
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

    protected synchronized void startApiQuery(String senderTag, ApiAction action, int priority) {
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
            if (holder.client != null && holder.client instanceof FeedbackApiClient) {
                ((FeedbackApiClient) holder.client).onProgress(data);
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

    public Map<String, RegisteredClient> getRegisteredClients() {
        return mRegisteredClients;
    }


    public static class RegisteredClient {
        public Deque<ApiAction> pendingActions;
        public List<ApiAction> runningActions;
        public Deque<ApiAction> completedActions;
        public ApiClient client;

        public RegisteredClient(ApiClient client) {
            setClient(client);
            pendingActions = new LinkedList<ApiAction>();
            runningActions = new ArrayList<ApiAction>();
            completedActions = new LinkedList<ApiAction>();
        }

        public void setClient(ApiClient client) {
            this.client = client;
        }
    }

    public interface Callback {
        public void onResponse(int actionCode, int remainingActions, Object response);
    }
}
