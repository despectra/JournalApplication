package com.despectra.android.journal.logic.local;

import android.content.Entity;
import android.net.Uri;
import android.provider.BaseColumns;
import com.despectra.android.journal.Data.DBHelper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmitry on 10.04.2014.
 */
public class Contract {
    //base
    public static final String AUTHORITY = "com.despectra.android.journal.provider";
    public static final String STRING_URI = "content://" + AUTHORITY;

    public static final String FIELD_ID = BaseColumns._ID;
    public static final String ENTITY_STATUS = "entity_status";

    //entity statuses
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_INSERTING = 1;
    public static final int STATUS_UPDATING = 2;
    public static final int STATUS_DELETING = 3;

    public static final String DIR_VND = "vnd.android.cursor.dir/vnd.";
    public static final String ITEM_VND = "vnd.android.cursor.item/vnd.";

    public static class Events implements EntityColumns {
        public static final EntityColumnsHolder HOLDER = new EntityColumnsHolder("Events");
        public static final String TABLE = "events";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String TABLE_JOIN_REMOTE = new DBHelper.JoinBuilder(TABLE).join(Remote.TABLE).onEq(Remote._ID, _ID).create();
        public static final String ENTITY_STATUS = TABLE + EntityColumns.ENTITY_STATUS;
        public static final String FIELD_TEXT = "events_text";
        public static final String FIELD_DATETIME = "events_datetime";
        public static final Uri URI = Uri.parse(STRING_URI + "/" + TABLE);
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 1;
        public static final int ID_URI_CODE = 2;

        public static class Remote implements RemoteColumns {
            public static final RemoteColumnsHolder HOLDER = new RemoteColumnsHolder("Events");
            public static final String TABLE = "events_remote";
            public static final String _ID = TABLE + RemoteColumns._ID;
            public static final String REMOTE_ID = TABLE + RemoteColumns.REMOTE_ID;
            public static final Uri URI = Uri.parse(STRING_URI + "/events_remote");
            public static final int URI_CODE = 3;
            public static final int ID_URI_CODE = 4;
        }
    }

    public static final class Groups implements BaseColumns {
        public static final EntityColumnsHolder HOLDER = new EntityColumnsHolder("Groups");
        public static final String TABLE = "groups";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String TABLE_JOIN_REMOTE = new DBHelper.JoinBuilder(TABLE).join(Remote.TABLE).onEq(Remote._ID, _ID).create();
        public static final String ENTITY_STATUS = TABLE + EntityColumns.ENTITY_STATUS;
        public static final String FIELD_NAME = "groups_name";
        public static final String FIELD_PARENT_ID = "groups_parent_id";
        public static final Uri URI = Uri.parse(STRING_URI + "/" + TABLE);
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 50;
        public static final int ID_URI_CODE = 51;

        public static final class Remote implements RemoteColumns {
            public static final RemoteColumnsHolder HOLDER = new RemoteColumnsHolder("Groups");
            public static final String TABLE = "groups_remote";
            public static final String _ID = TABLE + RemoteColumns._ID;
            public static final String REMOTE_ID = TABLE + RemoteColumns.REMOTE_ID;
            public static final Uri URI = Uri.parse(STRING_URI + "/groups_remote");
            public static final int URI_CODE = 52;
            public static final int ID_URI_CODE = 53;
        }
    }

    public static final class Students implements BaseColumns {
        public static final EntityColumnsHolder HOLDER = new EntityColumnsHolder("Students");
        public static final String TABLE = "students";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String TABLE_JOIN_REMOTE = new DBHelper.JoinBuilder(TABLE).join(Remote.TABLE).onEq(Remote._ID, _ID).create();
        public static final String ENTITY_STATUS = TABLE + EntityColumns.ENTITY_STATUS;
        public static final String FIELD_USER_ID = "students_user_id";
        public static final Uri URI = Uri.parse(STRING_URI + "/students");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 100;
        public static final int URI_BY_GROUP_CODE = 101;
        public static final int ID_URI_CODE = 102;
        public static final int ID_URI_BY_GROUP_CODE = 103;

        public static final class Remote implements RemoteColumns {
            public static final RemoteColumnsHolder HOLDER = new RemoteColumnsHolder("Students");
            public static final String TABLE = "students_remote";
            public static final String _ID = TABLE + RemoteColumns._ID;
            public static final String REMOTE_ID = TABLE + RemoteColumns.REMOTE_ID;
            public static final Uri URI = Uri.parse(STRING_URI + "/students_remote");
            public static final int URI_CODE = 104;
            public static final int URI_BY_GROUP_CODE = 105;
            public static final int ID_URI_CODE = 106;
        }
    }

    public static final class Users implements BaseColumns {
        public static final EntityColumnsHolder HOLDER = new EntityColumnsHolder("Users");
        public static final String TABLE = "users";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String TABLE_JOIN_REMOTE = new DBHelper.JoinBuilder(TABLE).join(Remote.TABLE).onEq(Remote._ID, _ID).create();
        public static final String ENTITY_STATUS = TABLE + EntityColumns.ENTITY_STATUS;
        public static final String FIELD_LOGIN = "users_login";
        public static final String FIELD_NAME = "users_name";
        public static final String FIELD_SURNAME = "users_surname";
        public static final String FIELD_MIDDLENAME = "users_middlename";
        public static final String FIELD_LEVEL = "users_level";
        public static final Uri URI = Uri.parse(STRING_URI + "/users");
        public static final Uri URI_STUDENTS = Uri.parse(STRING_URI + "/students/users");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 150;
        public static final int ID_URI_CODE = 151;
        public static final int URI_STUDENTS_CODE = 152;

        public static final class Remote implements RemoteColumns {
            public static final RemoteColumnsHolder HOLDER = new RemoteColumnsHolder("Users");
            public static final String TABLE = "users_remote";
            public static final String _ID = TABLE + RemoteColumns._ID;
            public static final String REMOTE_ID = TABLE + RemoteColumns.REMOTE_ID;
            public static final Uri URI = Uri.parse(STRING_URI + "/users_remote");
            public static final int URI_CODE = 153;
            public static final int ID_URI_CODE = 154;
        }
    }

    public static final class StudentsGroups implements BaseColumns {
        public static final EntityColumnsHolder HOLDER = new EntityColumnsHolder("StudentsGroups");
        public static final String TABLE = "students_groups";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String TABLE_JOIN_REMOTE = new DBHelper.JoinBuilder(TABLE).join(Remote.TABLE).onEq(Remote._ID, _ID).create();
        public static final String ENTITY_STATUS = TABLE + EntityColumns.ENTITY_STATUS;
        public static final String FIELD_STUDENT_ID = "students_groups_student_id";
        public static final String FIELD_GROUP_ID = "students_groups_group_id";
        public static final Uri URI = Uri.parse(STRING_URI + "/students_groups");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 200;
        public static final int ID_URI_CODE = 201;

        public static final class Remote implements RemoteColumns {
            public static final RemoteColumnsHolder HOLDER = new RemoteColumnsHolder("StudentsGroups");
            public static final String TABLE = "students_groups_remote";
            public static final String _ID = TABLE + RemoteColumns._ID;
            public static final String REMOTE_ID = TABLE + RemoteColumns.REMOTE_ID;
            public static final Uri URI = Uri.parse(STRING_URI + "/students_groups_remote");
            public static final int URI_CODE = 202;
            public static final int ID_URI_CODE = 203;
        }
    }

    public static final class Subjects implements BaseColumns {
        public static final EntityColumnsHolder HOLDER = new EntityColumnsHolder("Subjects");
        public static final String TABLE = "subjects";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String ENTITY_STATUS = TABLE + EntityColumns.ENTITY_STATUS;
        public static final String FIELD_NAME = "subjects_name";
        public static final Uri URI = Uri.parse(STRING_URI + "/subjects");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 250;
        public static final int ID_URI_CODE = 251;

        public static final class Remote implements RemoteColumns {
            public static final RemoteColumnsHolder HOLDER = new RemoteColumnsHolder("Subjects");
            public static final String TABLE = "subjects_remote";
            public static final String _ID = TABLE + RemoteColumns._ID;
            public static final String REMOTE_ID = TABLE + RemoteColumns.REMOTE_ID;
            public static final Uri URI = Uri.parse(STRING_URI + "/subjects_remote");
            public static final int URI_CODE = 252;
            public static final int ID_URI_CODE = 253;
        }
    }

    public static final class Marks implements BaseColumns {
        public static final EntityColumnsHolder HOLDER = new EntityColumnsHolder("Marks");
        public static final String TABLE = "marks";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String ENTITY_STATUS = TABLE + EntityColumns.ENTITY_STATUS;
        public static final String FIELD_STUDENT_ID = "marks_student_id";
        public static final String FIELD_LESSON_ID = "marks_lesson_id";
        public static final String FIELD_MARK = "marks_mark";
        public static final String TABLE_BY_GROUP = new DBHelper.JoinBuilder(TABLE).join(Students.TABLE).onEq(FIELD_STUDENT_ID, Students._ID)
                .join(StudentsGroups.TABLE).onEq(Students._ID, StudentsGroups.FIELD_STUDENT_ID).create();
        public static final Uri URI = Uri.parse(STRING_URI + "/marks");
        public static final Uri URI_BY_GROUP = Uri.parse(STRING_URI + "/marks/group");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 300;
        public static final int ID_URI_CODE = 301;
        public static final int URI_BY_GROUP_CODE = 302;
    }

    public static final class Lessons implements BaseColumns {
        public static final EntityColumnsHolder HOLDER = new EntityColumnsHolder("Lessons");
        public static final String TABLE = "lessons";
        public static final String _ID = TABLE + EntityColumns._ID;
        public static final String ENTITY_STATUS = TABLE + EntityColumns.ENTITY_STATUS;
        public static final String FIELD_DATE = "lessons_date";
        public static final String FIELD_TITLE = "lessons_title";
        public static final String FIELD_GROUP_ID = "lessons_group_id";
        public static final Uri URI = Uri.parse(STRING_URI + "/lessons");
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 350;
        public static final int ID_URI_CODE = 351;
    }

    public interface EntityColumns extends BaseColumns {
        public static final String TABLE = "";
        public static final String ENTITY_STATUS = "_entity_status";
    }

    public interface RemoteColumns extends BaseColumns {
        public static final String TABLE = "";
        public static final String _ID = "_local_id";
        public static final String REMOTE_ID = "_remote_id";
    }

    public static class EntityColumnsHolder {
        public String TABLE;
        public String _ID;
        public String ENTITY_STATUS;

        public EntityColumnsHolder(String contractClassName) {
            try {
                Class c = Class.forName("com.despectra.android.journal.logic.local.Contract$" + contractClassName);
                Field tableField = c.getDeclaredField("TABLE");
                Field idField = c.getDeclaredField("_ID");
                Field entityStatusField = c.getDeclaredField("ENTITY_STATUS");
                TABLE = (String)tableField.get(null);
                _ID = (String) idField.get(null);
                ENTITY_STATUS = (String) entityStatusField.get(null);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static class RemoteColumnsHolder {
        public String TABLE;
        public String _ID;
        public String REMOTE_ID;

        public RemoteColumnsHolder(String contractClassName) {
            try {
                Class c = Class.forName("com.despectra.android.journal.logic.local.Contract$" + contractClassName + "$Remote");
                Field tableField = c.getDeclaredField("TABLE");
                Field idField = c.getDeclaredField("_ID");
                Field remoteIdField = c.getDeclaredField("REMOTE_ID");
                TABLE = (String) tableField.get(null);
                _ID = (String) idField.get(null);
                REMOTE_ID = (String) remoteIdField.get(null);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
