package com.despectra.android.journal.logic.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.logic.queries.common.QueryExecutor;
import com.despectra.android.journal.logic.queries.common.QueryExecutorImpl;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.net.WebApiServer;
import com.despectra.android.journal.logic.ApiServiceHelper;
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

    private final Map<Integer, ActionImpl> mActionsImpls = new HashMap<Integer, ActionImpl>();

    public static final int MSG_RESPONSE = 0;
    public static final int MSG_PROGRESS = 1;

    private static WebApiServer mServer;
    private QueryExecutor mQueryExecutor;
    private ApiServiceBinder mBinder;
    private ApiServiceHelper mServiceHelper;
    private LocalStorageManager mUpdater;
    private Handler mResponseHandler;

    public ApiService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, String.format("Service OnCreate: %d", hashCode()));
        mServer = WebApiServer.instantiate(getApplicationContext(), getHostFromPreferences());
        mBinder = new ApiServiceBinder();
        JournalApplication application = (JournalApplication)getApplicationContext();
        mServiceHelper = application.getApiServiceHelper();
        mResponseHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (what == MSG_RESPONSE) {
                    Response response = (Response) msg.obj;
                    mServiceHelper.onServiceResponse(response);
                }
                if (what == MSG_PROGRESS) {
                    Pair<String, String> obj = (Pair<String, String>) msg.obj;
                    mServiceHelper.onServiceProgress(obj.first, obj.second);
                }
            }
        };
        mUpdater = new LocalStorageManager(getApplicationContext());
        mQueryExecutor = new QueryExecutorImpl(this, mServer, mResponseHandler);
        createActionsImplementations();
    }

    private void createActionsImplementations() {
        mActionsImpls.put(APICodes.ACTION_LOGIN, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.executePostApiQuery("auth.login", action.actionData);
            }
        });
        mActionsImpls.put(APICodes.ACTION_LOGOUT, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.executeGetApiQuery("auth.logout", action.actionData);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_MIN_PROFILE, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.executeGetApiQuery("profile.getMinProfile", action.actionData);
                //TODO load ava
            }
        });
        mActionsImpls.put(APICodes.ACTION_CHECK_TOKEN, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.executeGetApiQuery("auth.checkToken", action.actionData);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_INFO, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mServer.getServerInfo(action.actionData.getString("host"));
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_EVENTS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return  mQueryExecutor.forEvents().get(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_ADD_GROUP, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forGroups().add(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_GROUPS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forGroups().get(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_DELETE_GROUPS, new ActionImpl() {
            @Override
            public JSONObject doAction (ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forGroups().delete(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_UPDATE_GROUP, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forGroups().update(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_STUDENTS_BY_GROUP, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forStudents().getByGroup(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_ADD_STUDENT_IN_GROUP, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forStudents().addInGroup(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_DELETE_STUDENTS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forStudents().delete(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_SUBJECTS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forSubjects().get(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_ADD_SUBJECT, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forSubjects().add(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_UPDATE_SUBJECT, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forSubjects().update(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_DELETE_SUBJECTS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forSubjects().delete(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_ADD_TEACHER, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forTeachers().add(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_TEACHERS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forTeachers().get(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_DELETE_TEACHERS, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forTeachers().delete(action);
            }
        });
        mActionsImpls.put(APICodes.ACTION_GET_TEACHER, new ActionImpl() {
            @Override
            public JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
                return mQueryExecutor.forTeachers().getOne(action);
            }
        });
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

    public void processApiAction(final ApiServiceHelper.ApiAction action) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Response response = new Response();
                try {
                    response.initialAction = action;
                    int apiActionCode = action.apiCode;

                    JSONObject jsonResponse = doAction(action);

                    /*switch (apiActionCode) {

                        case 100500:
                            long g = jsonRequest.getLong("group");
                            String[] lessonsDates = new String[] {
                                    "10.04",
                                    "15.04",
                                    "16.04",
                                    "19.04",
                                    "21.04",
                                    "23.04"
                            };
                            String[] lessonIds = new String[6];
                            for (int i = 0; i < lessonIds.length; i++) {
                                ContentValues cv = new ContentValues();
                                cv.put(Contract.Lessons.FIELD_DATE, lessonsDates[i]);
                                cv.put(Contract.Lessons.FIELD_GROUP_ID, g);
                                cv.put(Contract.Lessons.FIELD_TITLE, "Generic lesson title");
                                lessonIds[i] = getContentResolver().insert(Contract.Lessons.URI, cv).getLastPathSegment();
                            }

                            Cursor students = getContentResolver().query(Uri.parse(String.format("%s/groups/%d/students", Contract.STRING_URI, g)),
                                    new String[]{Contract.Students._ID + " AS _id",},
                                    Contract.StudentsGroups.FIELD_GROUP_ID + " = ?",
                                    new String[]{String.valueOf(g)},
                                    Contract.Users.FIELD_SURNAME + " ASC");

                            students.moveToFirst();
                            do {
                                String student = students.getString(0);
                                for (int i = 0; i < lessonIds.length; i++) {
                                    String lesson = lessonIds[i];
                                    int mark = (int) Math.random() * 6;
                                    if (mark % 2 == 0) {
                                        mark = 0;
                                    } else {
                                        mark = (int) Math.random() * 3 + 3;
                                    }
                                    ContentValues cv = new ContentValues();
                                    cv.put(Contract.Marks.FIELD_LESSON_ID, lesson);
                                    cv.put(Contract.Marks.FIELD_STUDENT_ID, student);
                                    cv.put(Contract.Marks.FIELD_MARK, -1);
                                    getContentResolver().insert(Contract.Marks.URI, cv);
                                }
                            } while (students.moveToNext());

                            break;
                        case 100599:
                            String markId = jsonRequest.getString("markId");
                            String mark = jsonRequest.getString("mark");
                            ContentValues updCv = new ContentValues();
                            updCv.put(Contract.Marks.ENTITY_STATUS, Contract.STATUS_UPDATING);
                            updCv.put(Contract.Marks.FIELD_MARK, mark);
                            getContentResolver().update(Contract.Marks.URI,
                                    updCv,
                                    Contract.Marks._ID + " = " + markId,
                                    null);
                            //mServer.checkToken(jsonRequest);
                            SystemClock.sleep(7000);
                            ContentValues fnlCv = new ContentValues();
                            fnlCv.put(Contract.Marks.ENTITY_STATUS, Contract.STATUS_IDLE);
                            getContentResolver().update(Contract.Marks.URI, fnlCv, Contract.Marks._ID + " = " + markId, null);
                            break;
                    }*/
                    response.responseAction = new ApiServiceHelper.ApiAction(apiActionCode, action.clientTag, jsonResponse);
                    mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_RESPONSE, response));
                } catch (Exception ex) {
                    try {
                        String msg = ex.getMessage().replace("\"", "");
                        response.responseAction = new ApiServiceHelper.ApiAction(
                                action.apiCode,
                                action.clientTag,
                                new JSONObject(String.format("{\"success\":\"0\", \"error_code\":\"1\", \"error_message\":\"%s\"}", msg))
                        );
                    } catch (Exception e) {
                    }
                    mResponseHandler.sendMessage(Message.obtain(mResponseHandler, MSG_RESPONSE, response));
                }
            }
        }).start();
    }

    private JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception {
        if (mActionsImpls.containsKey(action.apiCode)) {
            return mActionsImpls.get(action.apiCode).doAction(action);
        } else {
            //No api action with given code
            return new JSONObject("{\"success\" : \"0\", \"error_code\" : \"-1\"}");
        }
    }



    private String getHostFromPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return preferences.getString(JournalApplication.PREFERENCE_KEY_HOST, "");
    }





    private interface ActionImpl {
        JSONObject doAction(ApiServiceHelper.ApiAction action) throws Exception;
    }

    public static class Response {
        public ApiServiceHelper.ApiAction initialAction;
        public ApiServiceHelper.ApiAction responseAction;
    }

    public class ApiServiceBinder extends Binder {
        public ApiService getService() {
            return ApiService.this;
        }
    }

}
