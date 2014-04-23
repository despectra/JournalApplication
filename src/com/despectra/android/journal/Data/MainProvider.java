package com.despectra.android.journal.Data;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Paint;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.despectra.android.journal.App.JournalApplication;
import org.apache.http.conn.ConnectionReleaseTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Dmitry on 01.04.14.
 */
public class MainProvider extends ContentProvider {

    private static final UriMatcher mMatcher;
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(Contract.AUTHORITY, "events", Contract.Events.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "events/#", Contract.Events.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "events_remote", Contract.Events.Remote.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups", Contract.Groups.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups_remote", Contract.Groups.Remote.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups_remote/#", Contract.Groups.Remote.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups_remote/#/students_remote", Contract.Students.Remote.URI_BY_GROUP_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#", Contract.Groups.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#/students", Contract.Students.URI_BY_GROUP_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#/students/#", Contract.Students.ID_URI_BY_GROUP_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students", Contract.Students.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students/#", Contract.Students.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_remote", Contract.Students.Remote.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_remote/#", Contract.Students.Remote.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students/users", Contract.Users.URI_STUDENTS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "users", Contract.Users.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "users/#", Contract.Users.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "users_remote", Contract.Users.Remote.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "users_remote/#", Contract.Users.Remote.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_groups", Contract.StudentsGroups.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_groups/#", Contract.StudentsGroups.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_groups_remote", Contract.StudentsGroups.Remote.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_groups_remote/#", Contract.StudentsGroups.Remote.ID_URI_CODE);
    }

    private static final SparseArray<String> mReadTables;
    static {
        mReadTables = new SparseArray<String>();

        mReadTables.append(Contract.Events.URI_CODE, Contract.Events.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.Events.ID_URI_CODE, Contract.Events.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.Events.Remote.URI_CODE, Contract.Events.Remote.TABLE);
        mReadTables.append(Contract.Events.Remote.ID_URI_CODE, Contract.Events.Remote.TABLE);

        mReadTables.append(Contract.Groups.URI_CODE, Contract.Groups.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.Groups.ID_URI_CODE, Contract.Groups.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.Groups.Remote.URI_CODE, Contract.Groups.Remote.TABLE);
        mReadTables.append(Contract.Groups.Remote.ID_URI_CODE, Contract.Groups.Remote.TABLE);

        mReadTables.append(Contract.Students.URI_CODE, Contract.Students.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.Students.ID_URI_CODE, Contract.Students.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.Students.Remote.URI_CODE, Contract.Students.Remote.TABLE);
        mReadTables.append(Contract.Students.Remote.ID_URI_CODE, Contract.Students.Remote.TABLE);
        mReadTables.append(Contract.Students.Remote.URI_BY_GROUP_CODE,
                new DBHelper.JoinBuilder(Contract.Users.Remote.TABLE)
                .join(Contract.Users.TABLE).onEq(Contract.Users.Remote._ID, Contract.Users._ID)
                .join(Contract.Students.TABLE).onEq(Contract.Users._ID, Contract.Students.FIELD_USER_ID)
                .join(Contract.Students.Remote.TABLE).onEq(Contract.Students.Remote._ID, Contract.Students._ID)
                .join(Contract.StudentsGroups.TABLE).onEq(Contract.StudentsGroups.FIELD_STUDENT_ID, Contract.Students._ID)
                .join(Contract.StudentsGroups.Remote.TABLE).onEq(Contract.StudentsGroups._ID, Contract.StudentsGroups.Remote._ID)
                .create());
        mReadTables.append(Contract.Students.URI_BY_GROUP_CODE,
                new DBHelper.JoinBuilder(Contract.Users.TABLE)
                .join(Contract.Students.TABLE).onEq(Contract.Users._ID, Contract.Students.FIELD_USER_ID)
                .join(Contract.StudentsGroups.TABLE).onEq(Contract.Students._ID, Contract.StudentsGroups.FIELD_STUDENT_ID)
                .create());

        mReadTables.append(Contract.Users.URI_CODE, Contract.Users.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.Users.ID_URI_CODE, Contract.Users.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.Users.URI_STUDENTS_CODE,
                new DBHelper.JoinBuilder(Contract.Users.TABLE)
                .join(Contract.Students.TABLE).onEq(Contract.Users._ID, Contract.Students.FIELD_USER_ID)
                .create());
        mReadTables.append(Contract.Users.Remote.URI_CODE, Contract.Users.Remote.TABLE);
        mReadTables.append(Contract.Users.Remote.ID_URI_CODE, Contract.Users.Remote.TABLE);

        mReadTables.append(Contract.StudentsGroups.URI_CODE, Contract.StudentsGroups.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.StudentsGroups.ID_URI_CODE, Contract.StudentsGroups.TABLE_JOIN_REMOTE);
        mReadTables.append(Contract.StudentsGroups.Remote.URI_CODE, Contract.StudentsGroups.Remote.TABLE);
        mReadTables.append(Contract.StudentsGroups.Remote.ID_URI_CODE, Contract.StudentsGroups.Remote.TABLE);
    }

    private static final SparseArray<String> mWriteTables;
    static {
        mWriteTables = new SparseArray<String>();

        mWriteTables.append(Contract.Events.URI_CODE, Contract.Events.TABLE);
        mWriteTables.append(Contract.Events.ID_URI_CODE, Contract.Events.TABLE);
        mWriteTables.append(Contract.Events.Remote.URI_CODE, Contract.Events.Remote.TABLE);
        mWriteTables.append(Contract.Events.Remote.ID_URI_CODE, Contract.Events.Remote.TABLE);

        mWriteTables.append(Contract.Groups.URI_CODE, Contract.Groups.TABLE);
        mWriteTables.append(Contract.Groups.ID_URI_CODE, Contract.Groups.TABLE);
        mWriteTables.append(Contract.Groups.Remote.URI_CODE, Contract.Groups.Remote.TABLE);
        mWriteTables.append(Contract.Groups.Remote.ID_URI_CODE, Contract.Groups.Remote.TABLE);

        mWriteTables.append(Contract.Students.URI_CODE, Contract.Students.TABLE);
        mWriteTables.append(Contract.Students.ID_URI_CODE, Contract.Students.TABLE);
        mWriteTables.append(Contract.Students.Remote.URI_CODE, Contract.Students.Remote.TABLE);
        mWriteTables.append(Contract.Students.Remote.ID_URI_CODE, Contract.Students.Remote.TABLE);

        mWriteTables.append(Contract.Users.URI_CODE, Contract.Users.TABLE);
        mWriteTables.append(Contract.Users.ID_URI_CODE, Contract.Users.TABLE);
        mWriteTables.append(Contract.Users.Remote.URI_CODE, Contract.Users.Remote.TABLE);
        mWriteTables.append(Contract.Users.Remote.ID_URI_CODE, Contract.Users.Remote.TABLE);

        mWriteTables.append(Contract.StudentsGroups.URI_CODE, Contract.StudentsGroups.TABLE);
        mWriteTables.append(Contract.StudentsGroups.ID_URI_CODE, Contract.StudentsGroups.TABLE);
        mWriteTables.append(Contract.StudentsGroups.Remote.URI_CODE, Contract.StudentsGroups.Remote.TABLE);
        mWriteTables.append(Contract.StudentsGroups.Remote.ID_URI_CODE, Contract.StudentsGroups.Remote.TABLE);
    }

    private static final SparseArray<String> mPrimaryColumns;
    static {
        mPrimaryColumns = new SparseArray<String>();

        mPrimaryColumns.append(Contract.Events.ID_URI_CODE, Contract.Events._ID);
        mPrimaryColumns.append(Contract.Events.Remote.ID_URI_CODE, Contract.Events.Remote._ID);

        mPrimaryColumns.append(Contract.Groups.ID_URI_CODE, Contract.Groups._ID);
        mPrimaryColumns.append(Contract.Groups.Remote.ID_URI_CODE, Contract.Groups.Remote._ID);

        mPrimaryColumns.append(Contract.Students.ID_URI_CODE, Contract.Students._ID);
        mPrimaryColumns.append(Contract.Students.Remote.ID_URI_CODE, Contract.Students.Remote._ID);

        mPrimaryColumns.append(Contract.Users.ID_URI_CODE, Contract.Users._ID);
        mPrimaryColumns.append(Contract.Users.Remote.ID_URI_CODE, Contract.Users.Remote._ID);

        mPrimaryColumns.append(Contract.StudentsGroups.ID_URI_CODE, Contract.StudentsGroups._ID);
        mPrimaryColumns.append(Contract.StudentsGroups.Remote.ID_URI_CODE, Contract.StudentsGroups.Remote._ID);
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
