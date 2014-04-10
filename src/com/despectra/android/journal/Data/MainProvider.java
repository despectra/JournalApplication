package com.despectra.android.journal.Data;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import com.despectra.android.journal.App.JournalApplication;
import org.apache.http.auth.AUTH;

/**
 * Created by Dmitry on 01.04.14.
 */
public class MainProvider extends ContentProvider {

    private static final UriMatcher mMatcher;
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(Contract.AUTHORITY, Contract.Events.TABLE, Contract.Events.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, Contract.Events.TABLE + "/#", Contract.Events.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, Contract.Groups.TABLE, Contract.Groups.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, Contract.Groups.TABLE + "/#", Contract.Groups.ID_URI_CODE);
    }

    private DBHelper mDbHelper;
    private SQLiteDatabase mDb;

    private String mDbName;
    private String mLogin;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String tableName;
        Uri notificationUri;
        switch (mMatcher.match(uri)) {
            case Contract.Events.URI_CODE:
                tableName = Contract.Events.TABLE;
                notificationUri = Contract.Events.URI;
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Contract.Events.FIELD_DATETIME + " DESC";
                }
                break;
            case Contract.Events.ID_URI_CODE:
                tableName = Contract.Events.TABLE;
                notificationUri = Contract.Events.URI;
                selection = prepareWhereClauseWithId(selection, uri.getLastPathSegment());
                break;
            case Contract.Groups.URI_CODE:
                tableName = Contract.Groups.TABLE;
                notificationUri = Contract.Groups.URI;
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Contract.Groups.FIELD_NAME + " ASC";
                }
                break;
            case Contract.Groups.ID_URI_CODE:
                tableName = Contract.Groups.TABLE;
                notificationUri = Contract.Groups.URI;
                selection = prepareWhereClauseWithId(selection, uri.getLastPathSegment());
                break;
            default:
                throw new IllegalStateException("Wrong URI while selecting: " + uri);
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
        result.setNotificationUri(getContext().getContentResolver(), notificationUri);
        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        String tableName;
        switch (mMatcher.match(uri)) {
            case Contract.Events.URI_CODE:
            case Contract.Events.ID_URI_CODE:
                tableName = Contract.Events.TABLE;
                break;
            case Contract.Groups.URI_CODE:
            case Contract.Groups.ID_URI_CODE:
                tableName = Contract.Groups.TABLE;
                break;
            default:
                throw new IllegalStateException("Wrong URI while inserting: " + uri);
        }
        mDb = getDBHelper().getWritableDatabase();
        long rowId = mDb.insert(
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
        String tableName;
        switch (mMatcher.match(uri)) {
            case Contract.Events.URI_CODE:
                tableName = Contract.Events.TABLE;
                break;
            case Contract.Events.ID_URI_CODE:
                tableName = Contract.Events.TABLE;
                selection = prepareWhereClauseWithId(selection, uri.getLastPathSegment());
                break;
            case Contract.Groups.URI_CODE:
                tableName = Contract.Groups.TABLE;
                break;
            case Contract.Groups.ID_URI_CODE:
                tableName = Contract.Groups.TABLE;
                selection = prepareWhereClauseWithId(selection, uri.getLastPathSegment());
                break;
            default:
                throw new IllegalStateException("Wrong URI while updating: " + uri);
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

    private String prepareWhereClauseWithId(String selection, String id) {
        if (selection == null) {
            selection = "";
        }
        if (!TextUtils.isEmpty(selection)) {
            selection += " AND ";
        }
        selection += "_id = " + id;
        return selection;
    }

    private static class DBHelper extends SQLiteOpenHelper {

        public static final int VERSION = 1;

        public static final String CREATE_TABLE_EVENTS = "" +
                "CREATE TABLE IF NOT EXISTS events (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "text TEXT, " +
                "datetime DATETIME NOT NULL)";

        public static final String CREATE_TABLE_GROUPS = "" +
                "CREATE TABLE IF NOT EXISTS groups (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "remote_id INTEGER DEFAULT 0 NOT NULL," +
                "name TEXT," +
                "parent_id INTEGER DEFAULT 0 NOT NULL," +
                "entity_status INTEGER DEFAULT 0 NOT NULL)";

        private String mName;

        private DBHelper(Context context, String name) {
            super(context, name, null, VERSION);
            mName = name;
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_TABLE_EVENTS);
            sqLiteDatabase.execSQL(CREATE_TABLE_GROUPS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        }
    }

}
