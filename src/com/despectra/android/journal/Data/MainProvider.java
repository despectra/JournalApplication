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

import java.sql.SQLException;

/**
 * Created by Dmitry on 01.04.14.
 */
public class MainProvider extends ContentProvider {
    //base
    public static final String AUTHORITY = JournalApplication.PACKAGE + ".provider";
    public static final String STRING_URI = "content://" + AUTHORITY;

    //tables
    public static final String TABLE_EVENTS = "events";

    //fields
    public static final String EVENTS_ID = BaseColumns._ID;
    public static final String EVENTS_TEXT = "text";
    public static final String EVENTS_DATETIME = "datetime";

    //URIs
    public static final Uri EVENTS_URI = Uri.parse(STRING_URI + "/" + TABLE_EVENTS);

    //data types
    public static final String DIR_VND = "vnd.android.cursor.dir/vnd.";
    public static final String ITEM_VND = "vnd.android.cursor.item/vnd.";

    public static final String EVENT_CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE_EVENTS;
    public static final String EVENT_CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE_EVENTS;

    //URIMatcher
    public static final int URI_EVENT = 1;
    public static final int URI_EVENT_ID = 2;
    private static final UriMatcher mMatcher;
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(AUTHORITY, TABLE_EVENTS, URI_EVENT);
        mMatcher.addURI(AUTHORITY, TABLE_EVENTS + "/#", URI_EVENT_ID);
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
        switch (mMatcher.match(uri)) {
            case URI_EVENT:
                tableName = TABLE_EVENTS;
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = EVENTS_DATETIME + " DESC";
                }
                break;
            case URI_EVENT_ID:
                tableName = TABLE_EVENTS;
                String id = uri.getLastPathSegment();
                if (selection == null) {
                    selection = "";
                }
                selection += ( !TextUtils.isEmpty(selection) ? " AND " : "") + EVENTS_ID + " = " + id;
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
        result.setNotificationUri(getContext().getContentResolver(), EVENTS_URI);
        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        String tableName;
        switch (mMatcher.match(uri)) {
            case URI_EVENT:
            case URI_EVENT_ID:
                tableName = TABLE_EVENTS;
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
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        String tableName;
        switch (mMatcher.match(uri)) {
            case URI_EVENT:
                tableName = TABLE_EVENTS;
                break;
            case URI_EVENT_ID:
                tableName = TABLE_EVENTS;
                String id = uri.getLastPathSegment();
                if (selection == null) {
                    selection = "";
                }
                selection += ( selection != null && !TextUtils.isEmpty(selection) ? " AND " : "") + EVENTS_ID + " = " + id;
                break;
            default:
                throw new IllegalStateException("Wrong URI while updating: " + uri);
        }
        mDb = getDBHelper().getWritableDatabase();
        int affected = mDb.update(tableName, contentValues, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return affected;
    }

    @Override
    public String getType(Uri uri) {
        switch (mMatcher.match(uri)) {
            case URI_EVENT:
                return EVENT_CONTENT_TYPE;
            case URI_EVENT_ID:
                return EVENT_CONTENT_ITEM_TYPE;
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

    private static class DBHelper extends SQLiteOpenHelper {

        public static final int VERSION = 1;

        public static final String CREATE_TABLE_EVENTS = "" +
                "CREATE TABLE IF NOT EXISTS events (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "text TEXT, " +
                "datetime DATETIME NOT NULL)";

        private String mName;

        private DBHelper(Context context, String name) {
            super(context, name, null, VERSION);
            mName = name;
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_TABLE_EVENTS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        }
    }

}
