package com.despectra.android.journal.logic.local;

import java.util.HashMap;
import java.util.Map;
import com.despectra.android.journal.logic.local.Contract.*;

/**
 * Created by Dmitry on 22.06.14.
 */
public class TableModel {
    private Map<String, EntityTable> mTables;
    private static TableModel sInstance;

    private TableModel() {
        mTables = new HashMap<String, EntityTable>();
        putTables();
        createDependencies();
    }

    private void putTables() {
        mTables.put(Events.TABLE, Events.HOLDER);
        mTables.put(Users.TABLE, Users.HOLDER);
        mTables.put(Groups.TABLE, Groups.HOLDER);
        mTables.put(Students.TABLE, Students.HOLDER);
        mTables.put(StudentsGroups.TABLE, StudentsGroups.HOLDER);
        mTables.put(Teachers.TABLE, Teachers.HOLDER);
        mTables.put(Subjects.TABLE, Subjects.HOLDER);
        mTables.put(TeachersSubjects.TABLE, TeachersSubjects.HOLDER);
        mTables.put(TSG.TABLE, TSG.HOLDER);
        mTables.put(Schedule.TABLE, Schedule.HOLDER);
    }

    private void createDependencies() {
        mTables.get(Groups.TABLE)
                .addDirectDependency(mTables.get(StudentsGroups.TABLE), StudentsGroups.FIELD_GROUP_ID)
                .addDirectDependency(mTables.get(TSG.TABLE), TSG.FIELD_GROUP_ID);
        mTables.get(Students.TABLE)
                .addDirectDependency(mTables.get(StudentsGroups.TABLE), StudentsGroups.FIELD_STUDENT_ID)
                .setBackDependency(mTables.get(Users.TABLE), Students.FIELD_USER_ID);
        mTables.get(Users.TABLE)
                .addDirectDependency(mTables.get(Teachers.TABLE), Teachers.FIELD_USER_ID)
                .addDirectDependency(mTables.get(Students.TABLE), Students.FIELD_USER_ID);
        mTables.get(StudentsGroups.TABLE)
                .setBackDependency(mTables.get(Students.TABLE), StudentsGroups.FIELD_STUDENT_ID);
        mTables.get(Subjects.TABLE)
                .addDirectDependency(mTables.get(TeachersSubjects.TABLE), TeachersSubjects.FIELD_SUBJECT_ID);
        mTables.get(Teachers.TABLE)
                .addDirectDependency(mTables.get(TeachersSubjects.TABLE), TeachersSubjects.FIELD_TEACHER_ID)
                .setBackDependency(mTables.get(Users.TABLE), Teachers.FIELD_USER_ID);
        mTables.get(TeachersSubjects.TABLE)
                .addDirectDependency(mTables.get(TSG.TABLE), TSG.FIELD_TEACHER_SUBJECT_ID);
        mTables.get(TSG.TABLE)
                .addDirectDependency(mTables.get(Schedule.TABLE), Schedule.FIELD_TSG_ID);
    }

    public synchronized static EntityTable getTable(String tableName) {
        if (sInstance == null) {
            sInstance = new TableModel();
        }
        return sInstance.mTables.get(tableName);
    }


}
