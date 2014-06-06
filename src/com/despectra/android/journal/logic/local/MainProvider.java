package com.despectra.android.journal.logic.local;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.SparseArray;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.utils.SQLJoinBuilder;

/**
 * Created by Dmitry on 01.04.14.
 */
public class MainProvider extends ContentProvider {

    private static final UriMatcher mMatcher;
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(Contract.AUTHORITY, "events", Contract.Events.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "events/#", Contract.Events.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups", Contract.Groups.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#", Contract.Groups.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#/students", Contract.Students.URI_BY_GROUP_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#/students/#", Contract.Students.ID_URI_BY_GROUP_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students", Contract.Students.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students/#", Contract.Students.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students/as_users", Contract.Students.URI_AS_USERS_CODE); //TODO check URI in usages
        mMatcher.addURI(Contract.AUTHORITY, "users", Contract.Users.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "users/#", Contract.Users.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_groups", Contract.StudentsGroups.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_groups/#", Contract.StudentsGroups.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "subjects", Contract.Subjects.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "subjects/#", Contract.Subjects.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers", Contract.Teachers.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers/as_users", Contract.Teachers.URI_AS_USERS_CODE); //TODO check URI in usages
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects", Contract.TeachersSubjects.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects/s", Contract.TeachersSubjects.URI_WITH_SUBJECTS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects/t", Contract.TeachersSubjects.URI_WITH_TEACHERS_CODE);

        /*mMatcher.addURI(Contract.AUTHORITY, "marks", Contract.Marks.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "marks/group", Contract.Marks.URI_BY_GROUP_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "lessons", Contract.Lessons.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "lessons/#", Contract.Lessons.ID_URI_CODE);*/
    }

    private static final SparseArray<String> mReadTables;
    static {
        mReadTables = new SparseArray<String>();

        mReadTables.append(Contract.Events.URI_CODE, Contract.Events.TABLE);
        mReadTables.append(Contract.Events.ID_URI_CODE, Contract.Events.TABLE);
        mReadTables.append(Contract.Groups.URI_CODE, Contract.Groups.TABLE);
        mReadTables.append(Contract.Groups.ID_URI_CODE, Contract.Groups.TABLE);
        mReadTables.append(Contract.Students.URI_CODE, Contract.Students.TABLE);
        mReadTables.append(Contract.Students.ID_URI_CODE, Contract.Students.TABLE);
        mReadTables.append(Contract.Students.URI_BY_GROUP_CODE, new SQLJoinBuilder(Contract.Students.TABLE)
                .join(Contract.Users.TABLE).onEq(Contract.Users._ID, Contract.Students.FIELD_USER_ID)
                .join(Contract.StudentsGroups.TABLE).onEq(Contract.StudentsGroups.FIELD_STUDENT_ID, Contract.Students._ID)
                .create());
        mReadTables.append(Contract.Students.URI_AS_USERS_CODE, new SQLJoinBuilder(Contract.Users.TABLE)
                .join(Contract.Students.TABLE).onEq(Contract.Users._ID, Contract.Students.FIELD_USER_ID)
                .create());
        mReadTables.append(Contract.Users.URI_CODE, Contract.Users.TABLE);
        mReadTables.append(Contract.Users.ID_URI_CODE, Contract.Users.TABLE);
        mReadTables.append(Contract.StudentsGroups.URI_CODE, Contract.StudentsGroups.TABLE);
        mReadTables.append(Contract.StudentsGroups.ID_URI_CODE, Contract.StudentsGroups.TABLE);
        mReadTables.append(Contract.Subjects.URI_CODE, Contract.Subjects.TABLE);
        mReadTables.append(Contract.Subjects.ID_URI_CODE, Contract.Subjects.TABLE);
        mReadTables.append(Contract.Teachers.URI_CODE, Contract.Teachers.TABLE);
        mReadTables.append(Contract.Teachers.URI_AS_USERS_CODE, Contract.Teachers.TABLE_JOIN_USERS);
        mReadTables.append(Contract.TeachersSubjects.URI_CODE, Contract.TeachersSubjects.TABLE_JOIN_SUBJECTS);
        mReadTables.append(Contract.TeachersSubjects.URI_WITH_SUBJECTS_CODE, Contract.TeachersSubjects.TABLE_JOIN_SUBJECTS);
        /*mReadTables.append(Contract.Marks.URI_BY_GROUP_CODE, Contract.Marks.TABLE_BY_GROUP);
        mReadTables.append(Contract.Lessons.URI_CODE, Contract.Lessons.TABLE);*/

    }

    private static final SparseArray<String> mWriteTables;
    static {
        mWriteTables = new SparseArray<String>();

        mWriteTables.append(Contract.Events.URI_CODE, Contract.Events.TABLE);
        mWriteTables.append(Contract.Events.ID_URI_CODE, Contract.Events.TABLE);
        mWriteTables.append(Contract.Groups.URI_CODE, Contract.Groups.TABLE);
        mWriteTables.append(Contract.Groups.ID_URI_CODE, Contract.Groups.TABLE);
        mWriteTables.append(Contract.Students.URI_CODE, Contract.Students.TABLE);
        mWriteTables.append(Contract.Students.ID_URI_CODE, Contract.Students.TABLE);
        mWriteTables.append(Contract.Users.URI_CODE, Contract.Users.TABLE);
        mWriteTables.append(Contract.Users.ID_URI_CODE, Contract.Users.TABLE);
        mWriteTables.append(Contract.StudentsGroups.URI_CODE, Contract.StudentsGroups.TABLE);
        mWriteTables.append(Contract.StudentsGroups.ID_URI_CODE, Contract.StudentsGroups.TABLE);
        mWriteTables.append(Contract.Subjects.URI_CODE, Contract.Subjects.TABLE);
        mWriteTables.append(Contract.Subjects.ID_URI_CODE, Contract.Subjects.TABLE);
        mWriteTables.append(Contract.Teachers.URI_CODE, Contract.Teachers.TABLE);
        mWriteTables.append(Contract.TeachersSubjects.URI_CODE, Contract.TeachersSubjects.TABLE);

       /* mWriteTables.append(Contract.Marks.URI_CODE, Contract.Marks.TABLE);
        mWriteTables.append(Contract.Marks.ID_URI_CODE, Contract.Marks.TABLE);
        mWriteTables.append(Contract.Lessons.URI_CODE, Contract.Lessons.TABLE);*/
    }

    private static final SparseArray<String> mPrimaryColumns;
    static {
        mPrimaryColumns = new SparseArray<String>();
        mPrimaryColumns.append(Contract.Events.ID_URI_CODE, Contract.Events._ID);
        mPrimaryColumns.append(Contract.Groups.ID_URI_CODE, Contract.Groups._ID);
        mPrimaryColumns.append(Contract.Students.ID_URI_CODE, Contract.Students._ID);
        mPrimaryColumns.append(Contract.Users.ID_URI_CODE, Contract.Users._ID);
        mPrimaryColumns.append(Contract.StudentsGroups.ID_URI_CODE, Contract.StudentsGroups._ID);
        mPrimaryColumns.append(Contract.Subjects.ID_URI_CODE, Contract.Subjects._ID);
        mPrimaryColumns.append(Contract.Teachers.ID_URI_CODE, Contract.Teachers._ID);
        mPrimaryColumns.append(Contract.TeachersSubjects.ID_URI_CODE, Contract.TeachersSubjects._ID);

        /*mPrimaryColumns.append(Contract.Marks.ID_URI_CODE, Contract.Marks._ID);
        mPrimaryColumns.append(Contract.Lessons.ID_URI_CODE, Contract.Lessons._ID);*/

    }

    private DBHelper mDbHelper;
    private SQLiteDatabase mDb;

    private String mLogin;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Uri notificationUri = uri;
        int matched = mMatcher.match(uri);
        String tableName = mReadTables.get(matched, "");
        if (tableName.isEmpty()) {
            throw new IllegalStateException("Wrong URI while selecting: " + uri);
        }
        String primaryColumn = mPrimaryColumns.get(matched, "");
        if (!primaryColumn.isEmpty()) {
            selection = prepareWhereClauseWithId(selection, primaryColumn, uri.getLastPathSegment());
        }
        mDb = getDBHelper().getReadableDatabase();
        Cursor result = mDb.query(
                tableName,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder,
                null
        );
        ContentResolver resolver = getContext().getContentResolver();
        result.setNotificationUri(resolver, notificationUri);
        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int matched = mMatcher.match(uri);
        String tableName = mWriteTables.get(matched, "");
        if (tableName.isEmpty()) {
            throw new IllegalStateException("Wrong URI while inserting: " + uri);
        }
        mDb = getDBHelper().getWritableDatabase();
        long rowId = mDb.insertOrThrow(
                tableName,
                null,
                contentValues
        );
        if (rowId <= 0) {
            throw new SQLiteException("Unable to insert row into " + uri);
        }
        Uri resultUri = ContentUris.withAppendedId(uri, rowId);
        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return updateOrDelete(false, uri, null, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        return updateOrDelete(true, uri, contentValues, selection, selectionArgs);
    }

    private int updateOrDelete(boolean doUpdate, Uri uri, ContentValues dataForUpdate, String selection, String[] selectionArgs) {
        int matched = mMatcher.match(uri);
        String tableName = mWriteTables.get(matched, "");
        if (tableName.isEmpty()) {
            throw new IllegalStateException("Wrong URI while updating of deleting: " + uri);
        }
        String primaryColumn = mPrimaryColumns.get(matched, "");
        if (!primaryColumn.isEmpty()) {
            selection = prepareWhereClauseWithId(selection, primaryColumn, uri.getLastPathSegment());
        }
        mDb = getDBHelper().getWritableDatabase();
        int affected = (doUpdate) ? mDb.update(tableName, dataForUpdate, selection, selectionArgs) : mDb.delete(tableName, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return affected;
    }

    @Override
    public String getType(Uri uri) {
        switch (mMatcher.match(uri)) {
            case Contract.Events.URI_CODE:
                return Contract.Events.CONTENT_TYPE;
            case Contract.Events.ID_URI_CODE:
                return Contract.Events.CONTENT_ITEM_TYPE;

            case Contract.Groups.URI_CODE:
                return Contract.Groups.CONTENT_TYPE;
            case Contract.Groups.ID_URI_CODE:
                return Contract.Groups.CONTENT_ITEM_TYPE;

            case Contract.Students.ID_URI_CODE:
            case Contract.Students.ID_URI_BY_GROUP_CODE:
                return Contract.Students.CONTENT_ITEM_TYPE;
            case Contract.Students.URI_BY_GROUP_CODE:
                return Contract.Students.CONTENT_TYPE;

            case Contract.Users.URI_CODE:
                return Contract.Users.CONTENT_TYPE;
            case Contract.Users.ID_URI_CODE:
                return Contract.Users.CONTENT_ITEM_TYPE;

            case Contract.StudentsGroups.URI_CODE:
                return Contract.StudentsGroups.CONTENT_TYPE;
            case Contract.StudentsGroups.ID_URI_CODE:
                return Contract.StudentsGroups.CONTENT_ITEM_TYPE;
        }
        return null;
    }

    private DBHelper getDBHelper() {
        String login = PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getString(JournalApplication.PREFERENCE_KEY_LOGIN, "");
        if (login.isEmpty()) {
            return null;
        }
        if (!login.equals(mLogin)) {
            mDbHelper = new DBHelper(getContext(), login);
            mLogin = login;
        }
        return mDbHelper;
    }

    private String prepareWhereClauseWithId(String selection, String idFieldName, String id) {
        if (selection == null) {
            selection = "";
        }
        if (!TextUtils.isEmpty(selection)) {
            selection += " AND ";
        }
        selection += idFieldName + " = " + id;
        return selection;
    }
}
