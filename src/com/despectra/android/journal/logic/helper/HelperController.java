package com.despectra.android.journal.logic.helper;

import com.despectra.android.journal.model.EntityIds;

/**
 * Created by Dmitry on 06.06.14.
 */
public interface HelperController {
    int getRunningActionsCount();

    int getLastRunningActionCode();

    void login(String login, String passwd, int priority);

    void logout(String token, int priority);

    void checkToken(String token, int priority);

    void getApiInfo(String host, int priority);

    void getMinProfile(String token, int priority);

    void getEvents(String token, int offset, int count, int priority);

    void getAllEvents(String token, int priority);

    void addGroup(String token, String name, EntityIds parentIds, int priority);

    void getAllGroups(String token, EntityIds parentIds, int priority);

    void getGroups(String token, EntityIds parentIds, int offset, int count, int priority);

    void deleteGroups(String token, EntityIds[] ids, int priority);

    void updateGroup(String token, EntityIds ids, String updName, EntityIds parentIds, int priority);

    void getStudentsByGroup(String token, EntityIds groupIds, int priority);

    void addStudentIntoGroup(String token, EntityIds groupIds, String name, String middlename, String surname, String login, int priority);

    void deleteStudents(String token, EntityIds[] ids, int priority);

    void getSubjects(String token, int offset, int count, int priority);

    void getAllSubjects(String token, int priority);

    void addSubject(String token, String name, int priority);

    void updateSubject(String token, EntityIds ids, String updName, int priority);

    void deleteSubjects(String token, EntityIds[] ids, int priority);

    void addTeacher(String token, String firstName, String middleName, String secondName, String login, int priority);

    void getTeachers(String token, int offset, int count, int priority);

    void deleteTeachers(String token, EntityIds[] ids, int priority);

    void getTeacher(String token, EntityIds userIds, EntityIds teacherIds, int priority);

    void getSubjectsOfTeacher(String token, EntityIds teacherIds, int priority);

    void setSubjectsOfTeacher(String token, EntityIds teacherIds, EntityIds[] subjectsIds, int priority);

    void unsetSubjectsOfTeacher(String token, EntityIds[] linksIds, int priority);

    void getGroupsOfTeachersSubject(String token, EntityIds teacherSubjectIds, int priority);

    void setGroupsOfTeachersSubject(String token, EntityIds teacherSubjectIds, EntityIds[] groupsIds, int priority);

    void unsetGroupsOfTeachersSubject(String token, EntityIds[] linksIds, int priority);

    // TEMPORARY
    void addMockMarks(long groupId);

    void updateMockMark(long markId, int mark);
}
