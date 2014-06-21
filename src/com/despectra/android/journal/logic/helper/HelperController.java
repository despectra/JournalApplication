package com.despectra.android.journal.logic.helper;

import android.content.Context;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.model.EntityIds;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmitry on 06.06.14.
 */
public abstract class HelperController {

    public static class Configurator {
        private HelperController mController;

        public Configurator(HelperController controller) {
            mController = controller;
        }

        public Configurator setLocalStorageManagerCallbacks(LocalStorageManager.PostCallbacks callbacks) {
            if (mController.mExtras == null) {
                mController.mExtras = new HashMap<String, Object>();
            }
            mController.addExtra("LSM_CALLBACKS", callbacks);
            return this;
        }

        public HelperController done() {
            return mController;
        }
    }

    private String mClientName;
    private ApiServiceHelper mHelper;
    private Map<String, Object> mExtras;

    public HelperController(Context appContext, String clientName) {
        mClientName = clientName;
        mHelper = ((JournalApplication)appContext).getApiServiceHelper();
    }

    public Configurator configure() {
        return new Configurator(this);
    }

    private void addExtra(String key, Object value) {
        mExtras.put(key, value);
    }

    public int getRunningActionsCount() {
        ApiServiceHelper.RegisteredClient holder = mHelper.getRegisteredClients().get(mClientName);
        return holder.runningActions.size();
    }

    public int getLastRunningActionCode() {
        if (getRunningActionsCount() == 1) {
            return mHelper.getRegisteredClients().get(mClientName).runningActions.get(0).apiCode;
        }
        return -1;
    }

    protected void runAction(int actionCode, JSONObject actionData, int priority) {
        ApiAction action = new ApiAction(actionCode, mClientName, actionData);
        if (mExtras != null) {
            action.setExtras(mExtras);
        }
        mHelper.startApiQuery(mClientName, action, priority);
        if (mExtras != null) {
            mExtras.clear();
        }
    }

    public abstract void login(String login, String passwd, int priority);

    public abstract void logout(String token, int priority);

    public abstract void checkToken(String token, int priority);

    public abstract void getApiInfo(String host, int priority);

    public abstract void getMinProfile(String token, int priority);

    public abstract void getEvents(String token, int offset, int count, int priority);

    public abstract void getAllEvents(String token, int priority);

    public abstract void addGroup(String token, String name, EntityIds parentIds, int priority);

    public abstract void getAllGroups(String token, EntityIds parentIds, int priority);

    public abstract void getGroups(String token, EntityIds parentIds, int offset, int count, int priority);

    public abstract void deleteGroups(String token, EntityIds[] ids, int priority);

    public abstract void updateGroup(String token, EntityIds ids, String updName, EntityIds parentIds, int priority);

    public abstract void getStudentsByGroup(String token, EntityIds groupIds, int priority);

    public abstract void addStudentIntoGroup(String token, EntityIds groupIds, String name, String middlename, String surname, String login, int priority);

    public abstract void deleteStudents(String token, EntityIds[] ids, int priority);

    public abstract void getSubjects(String token, int offset, int count, int priority);

    public abstract void getAllSubjects(String token, int priority);

    public abstract void addSubject(String token, String name, int priority);

    public abstract void updateSubject(String token, EntityIds ids, String updName, int priority);

    public abstract void deleteSubjects(String token, EntityIds[] ids, int priority);

    public abstract void addTeacher(String token, String firstName, String middleName, String secondName, String login, int priority);

    public abstract void getTeachers(String token, int offset, int count, int priority);

    public abstract void deleteTeachers(String token, EntityIds[] ids, int priority);

    public abstract void getTeacher(String token, EntityIds userIds, EntityIds teacherIds, int priority);

    public abstract void getSubjectsOfTeacher(String token, EntityIds teacherIds, int priority);

    public abstract void getSubjectsOfAllTeachers(String token, int priority);

    public abstract void setSubjectsOfTeacher(String token, EntityIds teacherIds, EntityIds[] subjectsIds, int priority);

    public abstract void unsetSubjectsOfTeacher(String token, EntityIds[] linksIds, int priority);

    public abstract void getGroupsOfTeachersSubject(String token, EntityIds teacherSubjectIds, int priority);

    public abstract void getGroupsOfAllTeachersSubjects(String token, int priority);

    public abstract void setGroupsOfTeachersSubject(String token, EntityIds teacherSubjectIds, EntityIds[] groupsIds, int priority);

    public abstract void unsetGroupsOfTeachersSubject(String token, EntityIds[] linksIds, int priority);

    public abstract void getWeekScheduleForGroup(String token, EntityIds groupIds, int priority);

    // TEMPORARY
    public abstract void addMockMarks(long groupId);

    public abstract void updateMockMark(long markId, int mark);
}
