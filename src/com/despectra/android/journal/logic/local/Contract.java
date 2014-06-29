package com.despectra.android.journal.logic.local;

import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;
import com.despectra.android.journal.utils.SQLJoinBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmitry on 10.04.2014.
 */
public final class Contract {
    //base
    public static final String AUTHORITY = "com.despectra.android.journal.provider";
    public static final String STRING_URI = "content://" + AUTHORITY;

    //entity statuses
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_INSERTING = 1;
    public static final int STATUS_UPDATING = 2;
    public static final int STATUS_DELETING = 3;

    public static final String DIR_VND = "vnd.android.cursor.dir/vnd.";
    public static final String ITEM_VND = "vnd.android.cursor.item/vnd.";

    public static class Events extends EntityColumns {
        public static final String TABLE = "events";
        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_TEXT = TABLE + "_text";
        public static final String FIELD_DATETIME = TABLE + "_datetime";

        public static final EntityTable HOLDER = new EntityTable("Events")
                .addDataField(REMOTE_ID, "event_id")
                .addDataField(FIELD_TEXT, "text")
                .addDataField(FIELD_DATETIME, "datetime")
                .addDataField(ENTITY_STATUS, "entity_status");
        public static final Uri URI = Uri.parse(STRING_URI + "/" + TABLE);
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 1;
        public static final int ID_URI_CODE = 2;
    }

    public static final class Groups extends EntityColumns {
        public static final String TABLE = "groups";
        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_NAME = TABLE + "_name";
        public static final String FIELD_PARENT_ID = TABLE + "_parent_id";

        public static final EntityTable HOLDER = new EntityTable("Groups")
                .addDataField(REMOTE_ID, "group_id")
                .addDataField(FIELD_NAME, "name")
                .addDataField(FIELD_PARENT_ID, "parent_id")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final Uri URI = Uri.parse(STRING_URI + "/" + TABLE);
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 50;
        public static final int ID_URI_CODE = 51;
    }

    public static final class Students extends EntityColumns {
        public static final String TABLE = "students";
        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_USER_ID = TABLE + "_user_id";

        public static final EntityTable HOLDER = new EntityTable("Students")
                .addDataField(REMOTE_ID, "student_id")
                .addDataField(FIELD_USER_ID, "user_id")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final String TABLE_JOIN_USERS = new SQLJoinBuilder(TABLE)
                .join(Users.TABLE).onEq(FIELD_USER_ID, Users._ID).create();
        public static final Uri URI = Uri.parse(STRING_URI + "/students");
        public static final Uri URI_AS_USERS = Uri.parse(STRING_URI + "/students/as_users");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 100;
        public static final int URI_BY_GROUP_CODE = 101;
        public static final int URI_AS_USERS_CODE = 104;
        public static final int ID_URI_CODE = 102;
        public static final int ID_URI_BY_GROUP_CODE = 103;
    }

    public static final class Users extends EntityColumns {
        public static final String TABLE = "users";

        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_LOGIN = TABLE + "_login";
        public static final String FIELD_FIRST_NAME = TABLE + "_first_name";
        public static final String FIELD_LAST_NAME = TABLE + "_last_name";
        public static final String FIELD_MIDDLE_NAME = TABLE + "_middle_name";
        public static final String FIELD_LEVEL = TABLE + "_level";

        public static final EntityTable HOLDER = new EntityTable("Users")
                .addDataField(REMOTE_ID, "user_id")
                .addDataField(FIELD_LOGIN, "login")
                .addDataField(FIELD_FIRST_NAME, "first_name")
                .addDataField(FIELD_LAST_NAME, "last_name")
                .addDataField(FIELD_MIDDLE_NAME, "middle_name")
                .addDataField(FIELD_LEVEL, "level")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final Uri URI = Uri.parse(STRING_URI + "/users");
        public static final Uri URI_STUDENTS = Uri.parse(STRING_URI + "/students/users");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 150;
        public static final int ID_URI_CODE = 151;
    }

    public static final class StudentsGroups extends EntityColumns {
        public static final String TABLE = "students_groups";
        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_STUDENT_ID = TABLE + "_student_id";
        public static final String FIELD_GROUP_ID = TABLE + "_group_id";

        public static final EntityTable HOLDER = new EntityTable("StudentsGroups")
                .addDataField(REMOTE_ID, "student_group_id")
                .addDataField(FIELD_GROUP_ID, "group_id")
                .addDataField(FIELD_STUDENT_ID, "student_id")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final Uri URI = Uri.parse(STRING_URI + "/students_groups");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 200;
        public static final int ID_URI_CODE = 201;

    }

    public static final class Subjects extends EntityColumns {
        public static final String TABLE = "subjects";

        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_NAME = TABLE + "_name";
        public static final Uri URI = Uri.parse(STRING_URI + "/subjects");
        public static final Uri URI_WITH_TSG = Uri.parse(STRING_URI+"/subjects/tsg");

        public static final EntityTable HOLDER = new EntityTable("Subjects")
                .addDataField(REMOTE_ID, "subject_id")
                .addDataField(FIELD_NAME, "name")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final String TABLE_JOIN_TSG = new SQLJoinBuilder(TABLE)
                .join(TeachersSubjects.TABLE).onEq(_ID, TeachersSubjects.FIELD_SUBJECT_ID)
                .join(TSG.TABLE).onEq(TeachersSubjects._ID, TSG.FIELD_TEACHER_SUBJECT_ID).create();

        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 250;
        public static final int URI_WITH_TSG_CODE = 252;
        public static final int ID_URI_CODE = 251;

    }

    public static final class Teachers extends EntityColumns {
        public static final String TABLE = "teachers";
        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_USER_ID = TABLE + "_user_id";

        public static final EntityTable HOLDER = new EntityTable("Teachers")
                .addDataField(REMOTE_ID, "teacher_id")
                .addDataField(FIELD_USER_ID, "user_id")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final String TABLE_JOIN_USERS = new SQLJoinBuilder(TABLE)
                .join(Users.TABLE).onEq(FIELD_USER_ID, Users._ID).create();
        public static final String TABLE_JOIN_TSG = new SQLJoinBuilder(TABLE_JOIN_USERS)
                .join(TeachersSubjects.TABLE).onEq(_ID, TeachersSubjects.FIELD_TEACHER_ID)
                .join(TSG.TABLE).onEq(TeachersSubjects._ID, TSG.FIELD_TEACHER_SUBJECT_ID).create();
        public static final Uri URI = Uri.parse(STRING_URI + "/teachers");
        public static final Uri URI_WITH_TSG = Uri.parse(STRING_URI + "/teachers/tsg");
        public static final Uri URI_AS_USERS = Uri.parse(STRING_URI + "/teachers/as_users");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 260;
        public static final int URI_AS_USERS_CODE = 262;
        public static final int URI_WITH_TSG_CODE = 263;
        public static final int ID_URI_CODE = 261;
    }

    public static final class TeachersSubjects extends EntityColumns {
        public static final String TABLE = "teachers_subjects";
        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_TEACHER_ID = TABLE + "_teacher_id";
        public static final String FIELD_SUBJECT_ID = TABLE + "_subject_id";

        public static final EntityTable HOLDER = new EntityTable("TeachersSubjects")
                .addDataField(REMOTE_ID, "teacher_subject_id")
                .addDataField(FIELD_SUBJECT_ID, "subject_id")
                .addDataField(FIELD_TEACHER_ID, "teacher_id")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final String TABLE_JOIN_SUBJECTS =  new SQLJoinBuilder(TABLE)
                .join(Subjects.TABLE).onEq(FIELD_SUBJECT_ID, Subjects._ID).create();
        public static final String TABLE_JOIN_TEACHERS = new SQLJoinBuilder(TABLE)
                .join(Teachers.TABLE).onEq(FIELD_TEACHER_ID, Teachers._ID).create();
        public static final Uri URI = Uri.parse(STRING_URI + "/teachers_subjects");
        public static final Uri URI_WITH_SUBJECTS = Uri.parse(STRING_URI + "/teachers_subjects/s");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 270;
        public static final int ID_URI_CODE = 271;
        public static final int URI_WITH_SUBJECTS_CODE = 273;
        public static final int URI_WITH_TEACHERS_CODE = 274;
    }

    public static final class TSG extends EntityColumns {
        public static final String TABLE = "teachers_subjects_groups";
        public static final String _ID = "tsg_id";
        public static final String REMOTE_ID = "tsg_remote_id";
        public static final String ENTITY_STATUS = "tsg_entity_status";
        public static final String FIELD_TEACHER_SUBJECT_ID = "tsg_teacher_subject_id";
        public static final String FIELD_GROUP_ID = "tsg_group_id";

        public static final EntityTable HOLDER = new EntityTable("TSG")
                .addDataField(REMOTE_ID, "teacher_subject_group_id")
                .addDataField(FIELD_TEACHER_SUBJECT_ID, "teacher_subject_id")
                .addDataField(FIELD_GROUP_ID, "group_id")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final String TABLE_JOIN_GROUPS = new SQLJoinBuilder(TABLE)
                .join(Groups.TABLE).onEq(FIELD_GROUP_ID, Groups._ID)
                .join(TeachersSubjects.TABLE).onEq(FIELD_TEACHER_SUBJECT_ID, TeachersSubjects._ID).create();
        public static final String TABLE_JOIN_TEACHERS_SUBJECTS = new SQLJoinBuilder(TABLE)
                .join(TeachersSubjects.TABLE).onEq(FIELD_TEACHER_SUBJECT_ID, TeachersSubjects._ID).create();
        public static final String TABLE_JOIN_SUBJECTS = new SQLJoinBuilder(TABLE_JOIN_TEACHERS_SUBJECTS)
                .join(Subjects.TABLE).onEq(TeachersSubjects.FIELD_SUBJECT_ID, Subjects._ID).create();
        public static final String TABLE_JOIN_TEACHERS_USERS = new SQLJoinBuilder(TABLE_JOIN_TEACHERS_SUBJECTS)
                .join(Teachers.TABLE).onEq(TeachersSubjects.FIELD_TEACHER_ID, Teachers._ID)
                .join(Users.TABLE).onEq(Teachers.FIELD_USER_ID, Users._ID).create();
        public static final String TABLE_JOIN_ALL = new SQLJoinBuilder(TABLE_JOIN_GROUPS)
                .join(Subjects.TABLE).onEq(TeachersSubjects.FIELD_SUBJECT_ID, Subjects._ID)
                .join(Teachers.TABLE).onEq(TeachersSubjects.FIELD_TEACHER_ID, Teachers._ID)
                .join(Users.TABLE).onEq(Teachers.FIELD_USER_ID, Users._ID).create();
        public static final Uri URI = Uri.parse(STRING_URI + "/teachers_subjects_groups");
        public static final Uri URI_WITH_GROUPS = Uri.parse(STRING_URI + "/teachers_subjects_groups/g");
        public static final Uri URI_WITH_TEACHERS = Uri.parse(STRING_URI + "/teachers_subjects_groups/t");
        public static final Uri URI_WITH_SUBJECTS = Uri.parse(STRING_URI + "/teachers_subjects_groups/s");
        public static final Uri URI_ALL = Uri.parse(STRING_URI + "/teachers_subjects_groups/all");
        public static final int URI_CODE = 300;
        public static final int ID_URI_CODE = 301;
        public static final int URI_WITH_GROUPS_CODE = 302;
        public static final int URI_WITH_TEACHERS_CODE = 303;
        public static final int URI_ALL_CODE = 305;

        public static final int URI_WITH_SUBJECTS_CODE = 304;
    }

    public static final class Schedule extends EntityColumns {
        public static final String TABLE = "schedule";
        public static final String _ID = TABLE + "_id";
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";
        public static final String FIELD_DAY = TABLE + "_day";
        public static final String FIELD_LESSON_NUMBER = TABLE + "_lesson_number";
        public static final String FIELD_TSG_ID = TABLE + "_teacher_subject_group_id";
        public static final String FIELD_COLOR = TABLE + "_color";

        public static final EntityTable HOLDER = new EntityTable("Schedule")
                .addDataField(REMOTE_ID, "schedule_item_id")
                .addDataField(FIELD_DAY, "day")
                .addDataField(FIELD_LESSON_NUMBER, "lesson_number")
                .addDataField(FIELD_TSG_ID, "teacher_subject_group_id")
                .addDataField(FIELD_COLOR, "color")
                .addDataField(ENTITY_STATUS, "entity_status");

        public static final String TABLE_JOIN_TSG = new SQLJoinBuilder(TABLE)
                .join(TSG.TABLE).onEq(FIELD_TSG_ID, TSG._ID).create();
        public static final String TABLE_JOIN_FULL = new SQLJoinBuilder(TABLE_JOIN_TSG)
                .join(Groups.TABLE).onEq(TSG.FIELD_GROUP_ID, Groups._ID)
                .join(TeachersSubjects.TABLE).onEq(TSG.FIELD_TEACHER_SUBJECT_ID, TeachersSubjects._ID)
                .join(Teachers.TABLE).onEq(TeachersSubjects.FIELD_TEACHER_ID, Teachers._ID)
                .join(Users.TABLE).onEq(Teachers.FIELD_USER_ID, Users._ID)
                .join(Subjects.TABLE).onEq(TeachersSubjects.FIELD_SUBJECT_ID, Subjects._ID)
                .create();
        public static final Uri URI = Uri.parse(STRING_URI + "/" + TABLE);
        public static final Uri URI_TSG = Uri.parse(STRING_URI + "/" + TABLE + "/tsg");

        public static final Uri URI_FULL = Uri.parse(STRING_URI + "/" + TABLE + "/full");
        public static final int URI_CODE = 350;
        public static final int ID_URI_CODE = 351;
        public static final int URI_FULL_CODE = 352;
        public static final int URI_TSG_CODE = 353;
    }

    //TODO I'll complete these contracts when time comes
    public static final class Lessons implements BaseColumns {
        public static final String TABLE = "lessons";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String REMOTE_ID = TABLE + "_" + EntityColumns.REMOTE_ID;
        public static final String ENTITY_STATUS = TABLE + "_" + EntityColumns.ENTITY_STATUS;

        public static final String FIELD_SCHED_ITEM_ID = TABLE + "_schedule_item_id";
        public static final String FIELD_DATE = TABLE + "_date";
        public static final String FIELD_TITLE = TABLE + "_title";
        public static final String FIELD_TYPE = TABLE + "_type";
        public static final String FIELD_HOMEWORK = TABLE + "_homework";
        public static final EntityTable HOLDER = new EntityTable("Lessons")
                .addDataField(REMOTE_ID, "lesson_id")
                .addDataField(FIELD_SCHED_ITEM_ID, "schedule_item_id")
                .addDataField(FIELD_DATE, "date")
                .addDataField(FIELD_TITLE, "title")
                .addDataField(FIELD_TYPE, "type")
                .addDataField(FIELD_HOMEWORK, "homework");

        public static final String TABLE_JOIN_SCHEDULE = new SQLJoinBuilder(TABLE)
                .join(Schedule.TABLE).onEq(FIELD_SCHED_ITEM_ID, Schedule._ID).create();
        public static final Uri URI = Uri.parse(STRING_URI + "/lessons");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 400;
        public static final int ID_URI_CODE = 401;
    }

    public static final class Marks implements BaseColumns {
        public static final String TABLE = "marks";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String REMOTE_ID = TABLE + "_remote_id";
        public static final String ENTITY_STATUS = TABLE + "_entity_status";

        public static final String FIELD_LESSON_ID = TABLE + "_lesson_id";
        public static final String FIELD_STUDENT_ID = TABLE + "_student_id";
        public static final String FIELD_MARK = TABLE + "_mark";
        public static final EntityTable HOLDER = new EntityTable("Marks")
                .addDataField(REMOTE_ID, "mark_id")
                .addDataField(FIELD_LESSON_ID, "lesson_id")
                .addDataField(FIELD_STUDENT_ID, "student_id")
                .addDataField(FIELD_MARK, "mark");

        public static final String TABLE_JOIN_LESSONS = new SQLJoinBuilder(TABLE)
                .join(Lessons.TABLE).onEq(FIELD_LESSON_ID, Lessons._ID).create();
        public static final Uri URI = Uri.parse(STRING_URI + "/marks");
        public static final Uri URI_WITH_LESSONS = Uri.parse(STRING_URI + "/marks/lessons");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 450;
        public static final int ID_URI_CODE = 451;
    }

    public static class EntityColumns implements BaseColumns {
        public static final String REMOTE_ID = "remote_id";
        public static final String ENTITY_STATUS = "entity_status";
    }

    public static class EntityTable {
        public Uri URI;
        public String TABLE;
        public String _ID;
        public String REMOTE_ID;
        public String ENTITY_STATUS;
        public Map<EntityTable, String> DIRECT_DEPENDENCIES;
        public Pair<EntityTable, String> BACK_DEPENDENCY;
        public BiMap<String, String> DATA_FIELDS;

        public EntityTable(String contractClassName) {
            try {
                Class c = Class.forName("com.despectra.android.journal.logic.local.Contract$" + contractClassName);
                Field tableField = c.getDeclaredField("TABLE");
                Field idField = c.getDeclaredField("_ID");
                Field remoteIdField = c.getDeclaredField("REMOTE_ID");
                Field entityStatusField = c.getDeclaredField("ENTITY_STATUS");

                TABLE = (String)tableField.get(null);
                URI = Uri.parse(Contract.STRING_URI + "/" + TABLE);
                REMOTE_ID = (String) remoteIdField.get(null);
                _ID = (String) idField.get(null);
                ENTITY_STATUS = (String) entityStatusField.get(null);
                DIRECT_DEPENDENCIES = new HashMap<EntityTable, String>();
                DATA_FIELDS = HashBiMap.create();
            } catch (Exception e) {
                Log.e("Contract", e.getMessage(), e);
            }
        }

        public EntityTable addDirectDependency(EntityTable table, String foreignKey) {
            DIRECT_DEPENDENCIES.put(table, foreignKey);
            return this;
        }

        public EntityTable setBackDependency(EntityTable table, String myForeignKey) {
            BACK_DEPENDENCY = new Pair<Contract.EntityTable, String>(table, myForeignKey);
            return this;
        }

        public EntityTable addDataField(String localField, String remoteField) {
            DATA_FIELDS.put(localField, remoteField);
            return this;
        }
    }
}
