package com.despectra.android.journal.logic.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Dmitry on 15.04.2014.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final int VERSION = 27;

    public static final String CREATE_TABLE_EVENTS = "" +
            "CREATE TABLE IF NOT EXISTS events (" +
            "events_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "events_remote_id INTEGER DEFAULT 0 NOT NULL, " +
            "events_text TEXT, " +
            "events_datetime DATETIME NOT NULL, " +
            "events_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_GROUPS = "" +
            "CREATE TABLE IF NOT EXISTS groups (" +
            "groups_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "groups_remote_id INTEGER DEFAULT 0 NOT NULL, " +
            "groups_name TEXT, " +
            "groups_parent_id INTEGER DEFAULT 0 NOT NULL, " +
            "groups_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_STUDENTS = "" +
            "CREATE TABLE IF NOT EXISTS students (" +
            "students_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "students_remote_id INTEGER DEFAULT 0 NOT NULL, " +
            "students_user_id INTEGER NOT NULL, " +
            "students_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_USERS = "" +
            "CREATE TABLE IF NOT EXISTS users (" +
            "users_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "users_remote_id INTEGER DEFAULT 0 NOT NULL, " +
            "users_login TEXT NOT NULL, " +
            "users_first_name TEXT NOT NULL, " +
            "users_middle_name TEXT NOT NULL, " +
            "users_last_name TEXT NOT NULL, " +
            "users_level INTEGER NOT NULL, " +
            "users_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_STUDENTS_GROUPS = "" +
            "CREATE TABLE IF NOT EXISTS students_groups (" +
            "students_groups_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "students_groups_remote_id INTEGER DEFAULT 0 NOT NULL, " +
            "students_groups_group_id INTEGER NOT_NULL, " +
            "students_groups_student_id INTEGER NOT NULL, " +
            "students_groups_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_SUBJECTS = "" +
            "CREATE TABLE IF NOT EXISTS subjects (" +
            "subjects_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "subjects_remote_id INTEGER DEFAULT 0 NOT NULL," +
            "subjects_name TEXT NOT NULL," +
            "subjects_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_TEACHERS = "" +
            "CREATE TABLE IF NOT EXISTS teachers (" +
            "teachers_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "teachers_remote_id INTEGER DEFAULT 0 NOT NULL," +
            "teachers_user_id INTEGER NOT NULL," +
            "teachers_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_TEACHERS_SUBJECTS = "" +
            "CREATE TABLE IF NOT EXISTS teachers_subjects (" +
            "teachers_subjects_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "teachers_subjects_remote_id INTEGER DEFAULT 0 NOT NULL," +
            "teachers_subjects_teacher_id INTEGER NOT NULL," +
            "teachers_subjects_subject_id INTEGER NOT NULL," +
            "teachers_subjects_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_TEACHERS_SUBJECTS_GROUPS = "" +
            "CREATE TABLE IF NOT EXISTS teachers_subjects_groups (" +
            "tsg_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "tsg_remote_id INTEGER DEFAULT 0 NOT NULL," +
            "tsg_teacher_subject_id INTEGER NOT NULL," +
            "tsg_group_id INTEGER NOT NULL," +
            "tsg_entity_status INTEGER DEFAULT 0 NOT NULL)";

    public static final String CREATE_TABLE_SCHEDULE = "" +
            "CREATE TABLE IF NOT EXISTS schedule (" +
            "schedule_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "schedule_remote_id INTEGER DEFAULT 0 NOT NULL," +
            "schedule_day INTEGER NOT NULL," +
            "schedule_lesson_number INTEGER NOT NULL," +
            "schedule_teacher_subject_group_id INTEGER NOT NULL," +
            "schedule_color INTEGER NOT NULL," +
            "schedule_entity_status INTEGER DEFAULT 0 NOT NULL)";

/*    public static final String CREATE_TABLE_LESSONS = "" +
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
            "marks_mark TEXT NOT NULL)";*/

    public static final String DROP_TABLE = "" +
            "DROP TABLE %s";
    private String mName;

    public DBHelper(Context context, String name) {
        super(context, name, null, VERSION);
        mName = name;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        buildSchema(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (newVersion - oldVersion == 1) {
            /*dropSchema(sqLiteDatabase);
            buildSchema(sqLiteDatabase);*/
            sqLiteDatabase.execSQL(CREATE_TABLE_SCHEDULE);
        }
    }

    private void dropSchema(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "events"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "groups"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "students"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "users"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "students_groups"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "subjects"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "teachers"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "teachers_subjects"));
        sqLiteDatabase.execSQL(String.format(DROP_TABLE, "teachers_subjects_groups"));

    }

    private void buildSchema(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE_EVENTS);
        sqLiteDatabase.execSQL(CREATE_TABLE_GROUPS);
        sqLiteDatabase.execSQL(CREATE_TABLE_STUDENTS);
        sqLiteDatabase.execSQL(CREATE_TABLE_USERS);
        sqLiteDatabase.execSQL(CREATE_TABLE_STUDENTS_GROUPS);
        sqLiteDatabase.execSQL(CREATE_TABLE_SUBJECTS);
        sqLiteDatabase.execSQL(CREATE_TABLE_TEACHERS);
        sqLiteDatabase.execSQL(CREATE_TABLE_TEACHERS_SUBJECTS);
        sqLiteDatabase.execSQL(CREATE_TABLE_TEACHERS_SUBJECTS_GROUPS);
        sqLiteDatabase.execSQL(CREATE_TABLE_SCHEDULE);
    }
}
