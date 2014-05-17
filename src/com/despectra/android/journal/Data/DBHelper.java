package com.despectra.android.journal.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Dmitry on 15.04.2014.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final int VERSION = 23;

    public static final String CREATE_TABLE_ENTITY_REMOTE = "" +
            "CREATE TABLE IF NOT EXISTS %1$s_remote (" +
            "%1$s_remote_local_id INTEGER PRIMARY KEY," +
            "%1$s_remote_remote_id INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_EVENTS = "" +
            "CREATE TABLE IF NOT EXISTS events (" +
            "events_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "events_text TEXT, " +
            "events_datetime DATETIME NOT NULL, " +
            "events_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_GROUPS = "" +
            "CREATE TABLE IF NOT EXISTS groups (" +
            "groups_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "groups_name TEXT, " +
            "groups_parent_id INTEGER DEFAULT 0 NOT NULL, " +
            "groups_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_STUDENTS = "" +
            "CREATE TABLE IF NOT EXISTS students (" +
            "students_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "students_user_id INTEGER NOT NULL, " +
            "students_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_USERS = "" +
            "CREATE TABLE IF NOT EXISTS users (" +
            "users_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "users_login TEXT NOT NULL, " +
            "users_name TEXT NOT NULL, " +
            "users_middlename TEXT NOT NULL, " +
            "users_surname TEXT NOT NULL, " +
            "users_level INTEGER NOT NULL, " +
            "users_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_STUDENTS_GROUPS = "" +
            "CREATE TABLE IF NOT EXISTS students_groups (" +
            "students_groups_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "students_groups_group_id INTEGER NOT_NULL, " +
            "students_groups_student_id INTEGER NOT NULL, " +
            "students_groups_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_SUBJECTS = "" +
            "CREATE TABLE IF NOT EXISTS subjects (" +
            "subjects_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "subjects_name TEXT NOT NULL)";

    public static final String CREATE_TABLE_LESSONS = "" +
            "CREATE TABLE IF NOT EXISTS lessons (" +
            "lessons_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "lessons_group_id INTEGER NOT NULL," +
            "lessons_date TEXT NOT NULL," +
            "lessons_title TEXT NOT NULL)";

    public static final String CREATE_TABLE_MARKS = "" +
            "CREATE TABLE IF NOT EXISTS marks (" +
            "marks_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "marks_lesson_id INTEGER NOT NULL," +
            "marks_student_id INTEGER NOT NULL," +
            "marks_entity_status INTEGER DEFAULT 0 NOT NULL," +
            "marks_mark TEXT NOT NULL)";

    public static final String DROP_TABLE = "" +
            "DROP TABLE %s";
    private String mName;

    public DBHelper(Context context, String name) {
        super(context, name, null, VERSION);
        mName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        buildSchedma(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (newVersion - oldVersion == 1) {
            dropSchema(sqLiteDatabase);
            buildSchedma(sqLiteDatabase);
        }
    }

    private void dropSchema(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "events"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "groups"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "students"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "users"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "students_groups"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "events_remote"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "groups_remote"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "students_remote"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "users_remote"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "students_groups_remote"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "subjects"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "subjects_remote"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "lessons"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "marks"));
    }

    private void buildSchedma(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE_EVENTS);
        sqLiteDatabase.execSQL(String.format(CREATE_TABLE_ENTITY_REMOTE, "events"));
        sqLiteDatabase.execSQL(CREATE_TABLE_GROUPS);
        sqLiteDatabase.execSQL(String.format(CREATE_TABLE_ENTITY_REMOTE, "groups"));
        sqLiteDatabase.execSQL(CREATE_TABLE_STUDENTS);
        sqLiteDatabase.execSQL(String.format(CREATE_TABLE_ENTITY_REMOTE, "students"));
        sqLiteDatabase.execSQL(CREATE_TABLE_USERS);
        sqLiteDatabase.execSQL(String.format(CREATE_TABLE_ENTITY_REMOTE, "users"));
        sqLiteDatabase.execSQL(CREATE_TABLE_STUDENTS_GROUPS);
        sqLiteDatabase.execSQL(String.format(CREATE_TABLE_ENTITY_REMOTE, "students_groups"));
        sqLiteDatabase.execSQL(CREATE_TABLE_SUBJECTS);
        sqLiteDatabase.execSQL(String.format(CREATE_TABLE_ENTITY_REMOTE, "subjects"));

        sqLiteDatabase.execSQL(CREATE_TABLE_LESSONS);
        sqLiteDatabase.execSQL(CREATE_TABLE_MARKS);
    }

    public static class JoinBuilder {
        private String mJoinString;
        public JoinBuilder(String initTable) {
            mJoinString = new String();
            mJoinString += initTable;
        }

        public JoinBuilder join(String table) {
            mJoinString += " JOIN " + table;
            return this;
        }

        public JoinBuilder on(String predicate) {
            mJoinString += " ON " + predicate;
            return this;
        }

        public JoinBuilder onEq(String column1, String column2) {
            mJoinString += " ON " + column1 + " = " + column2;
            return this;
        }

        public String create() {
            return mJoinString;
        }
    }

}
