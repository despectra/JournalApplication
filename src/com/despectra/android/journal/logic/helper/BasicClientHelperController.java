package com.despectra.android.journal.logic.helper;

import android.content.Context;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.utils.JSONBuilder;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.logic.helper.ApiServiceHelper.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmitry on 19.06.14.
 */
public class BasicClientHelperController extends HelperController {

    public BasicClientHelperController(Context appContext, String clientName) {
        super(appContext, clientName);
    }

    @Override
    public void login(String login, String passwd, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("login", login)
                .addKeyValue("passwd", Utils.md5(passwd))
                .create();
        runAction(APICodes.ACTION_LOGIN, data, priority);
    }

    @Override
    public void logout(String token, int priority) {
        JSONObject data = new JSONBuilder().addKeyValue("token", token).create();
        runAction(APICodes.ACTION_LOGOUT, data, priority);
    }

    @Override
    public void checkToken(String token, int priority) {
        JSONObject data = new JSONBuilder().addKeyValue("token", token).create();
        runAction(APICodes.ACTION_CHECK_TOKEN, data, priority);
    }

    @Override
    public void getApiInfo(String host, int priority) {
        JSONObject data = new JSONBuilder().addKeyValue("host", host).create();
        runAction(APICodes.ACTION_GET_INFO, data, priority);
    }

    @Override
    public void getMinProfile(String token, int priority) {
        JSONObject data = new JSONBuilder().addKeyValue("token", token).create();
        runAction(APICodes.ACTION_GET_MIN_PROFILE, data, priority);
    }

    @Override
    public void getEvents(String token, int offset, int count, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addKeyValue("count", count)
                .addKeyValue("offset", offset).create();
        runAction(APICodes.ACTION_GET_EVENTS, data, priority);
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
        runAction(APICodes.ACTION_ADD_GROUP, data, priority);
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
        runAction(APICodes.ACTION_GET_GROUPS, data, priority);
    }

    @Override
    public void deleteGroups(String token, EntityIds[] ids, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIdsArray("groups", ids).create();
        runAction(APICodes.ACTION_DELETE_GROUPS, data, priority);
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
        runAction(APICodes.ACTION_UPDATE_GROUP, data, priority);
    }

    @Override
    public void getStudentsByGroup(String token, EntityIds groupIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("group_id", groupIds)
                .create();
        runAction(APICodes.ACTION_GET_STUDENTS_BY_GROUP, data, priority);
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
        runAction(APICodes.ACTION_ADD_STUDENT_IN_GROUP, data, priority);
    }

    @Override
    public void deleteStudents(String token, EntityIds[] ids, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIdsArray("students", ids)
                .create();
        runAction(APICodes.ACTION_DELETE_STUDENTS, data, priority);
    }

    @Override
    public void getSubjects(String token, int offset, int count, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addKeyValue("offset", offset)
                .addKeyValue("count", count).create();
        runAction(APICodes.ACTION_GET_SUBJECTS, data, priority);
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
        runAction(APICodes.ACTION_ADD_SUBJECT, data, priority);
    }

    @Override
    public void updateSubject(String token, EntityIds ids, String updName, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("id", ids)
                .addKeyValue("data", new JSONBuilder()
                        .addKeyValue("name", updName).create()).create();
        runAction(APICodes.ACTION_UPDATE_SUBJECT, data, priority);
    }

    @Override
    public void deleteSubjects(String token, EntityIds[] ids, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIdsArray("subjects", ids)
                .create();
        runAction(APICodes.ACTION_DELETE_SUBJECTS, data, priority);
    }

    @Override
    public void addTeacher(String token, String firstName, String middleName, String secondName, String login, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addKeyValue("firstName", firstName)
                .addKeyValue("middleName", middleName)
                .addKeyValue("secondName", secondName)
                .addKeyValue("login", login).create();
        runAction(APICodes.ACTION_ADD_TEACHER, data, priority);
    }

    @Override
    public void getTeachers(String token, int offset, int count, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addKeyValue("offset", offset)
                .addKeyValue("count", count).create();
        runAction(APICodes.ACTION_GET_TEACHERS, data, priority);
    }

    @Override
    public void deleteTeachers(String token, EntityIds[] ids, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIdsArray("teachers", ids)
                .create();
        runAction(APICodes.ACTION_DELETE_TEACHERS, data, priority);
    }

    @Override
    public void getTeacher(String token, EntityIds userIds, EntityIds teacherIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addKeyValue("user_id", userIds.getLocalId())
                .addEntityIds("teacher_id", teacherIds)
                .create();
        runAction(APICodes.ACTION_GET_TEACHER, data, priority);
    }

    @Override
    public void getSubjectsOfTeacher(String token, EntityIds teacherIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("teacher_id", teacherIds)
                .create();
        runAction(APICodes.ACTION_GET_SUBJECTS_OF_TEACHER, data, priority);
    }

    @Override
    public void getSubjectsOfAllTeachers(String token, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token).create();
        runAction(APICodes.ACTION_GET_SUBJECTS_OF_ALL_TEACHERS, data, priority);
    }

    @Override
    public void setSubjectsOfTeacher(String token, EntityIds teacherIds, EntityIds[] subjectsIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("teacher_id", teacherIds)
                .addEntityIdsArray("subjects_ids", subjectsIds).create();
        runAction(APICodes.ACTION_SET_SUBJECTS_OF_TEACHER, data, priority);
    }

    @Override
    public void unsetSubjectsOfTeacher(String token, EntityIds[] linksIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIdsArray("links_ids", linksIds).create();
        runAction(APICodes.ACTION_UNSET_SUBJECTS_OF_TEACHER, data, priority);
    }

    @Override
    public void getGroupsOfTeachersSubject(String token, EntityIds teacherSubjectIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("teacher_subject_id", teacherSubjectIds)
                .create();
        runAction(APICodes.ACTION_GET_GROUPS_OF_TEACHERS_SUBJECT, data, priority);
    }

    @Override
    public void getGroupsOfAllTeachersSubjects(String token, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .create();
        runAction(APICodes.ACTION_GET_GROUPS_OF_ALL_TS, data, priority);
    }

    @Override
    public void setGroupsOfTeachersSubject(String token, EntityIds teacherSubjectIds, EntityIds[] groupsIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("teacher_subject_id", teacherSubjectIds)
                .addEntityIdsArray("groups_ids", groupsIds).create();
        runAction(APICodes.ACTION_SET_GROUPS_OF_TEACHERS_SUBJECT, data, priority);
    }

    @Override
    public void unsetGroupsOfTeachersSubject(String token, EntityIds[] linksIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIdsArray("links_ids", linksIds).create();
        runAction(APICodes.ACTION_UNSET_GROUPS_OF_TEACHERS_SUBJECT, data, priority);
    }

    @Override
    public void getWeekScheduleForGroup(String token, EntityIds groupIds, int priority) {
        JSONObject data = new JSONBuilder()
                .addKeyValue("token", token)
                .addEntityIds("group_id", groupIds).create();
        runAction(APICodes.ACTION_GET_WEEK_SCHEDULE_FOR_GROUP, data, priority);
    }

    @Override
    public void addMockMarks(long groupId) {
        /*if (mBound) {
            try {
                mService.processApiAction(new ApiAction(100500, mClientName, new JSONObject(String.format("{\"group\":\"%d\"}", groupId))));
            } catch (JSONException e) {

            }
        }*/
    }

    @Override
    public void updateMockMark(long markId, int mark) {
        /*if (mBound) {
            try {
                JSONObject json = new JSONObject();
                json.put("markId", markId);
                json.put("mark", mark);
                mService.processApiAction(new ApiAction(100599, mClientName, json));
            } catch (JSONException e) {

            }
        }*/
    }


}
