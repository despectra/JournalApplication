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
import com.despectra.android.journal.logic.local.Contract.*;

import java.util.ArrayList;

/**
 * Created by Dmitry on 01.04.14.
 */
public class MainProvider extends ContentProvider {

    private static final UriMatcher mMatcher;
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(Contract.AUTHORITY, "events", Events.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "events/#", Events.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups", Groups.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#", Groups.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#/students", Students.URI_BY_GROUP_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "groups/#/students/#", Students.ID_URI_BY_GROUP_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students", Students.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students/#", Students.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students/as_users", Students.URI_AS_USERS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "users", Users.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "users/#", Users.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_groups", StudentsGroups.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "students_groups/#", StudentsGroups.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "subjects", Subjects.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "subjects/#", Subjects.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers", Teachers.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers/as_users", Teachers.URI_AS_USERS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers/tsg", Teachers.URI_WITH_TSG_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects", TeachersSubjects.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects/s", TeachersSubjects.URI_WITH_SUBJECTS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects/t", TeachersSubjects.URI_WITH_TEACHERS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects_groups", TSG.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects_groups/g", TSG.URI_WITH_GROUPS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects_groups/t", TSG.URI_WITH_TEACHERS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "teachers_subjects_groups/s", TSG.URI_WITH_SUBJECTS_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "schedule", Schedule.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "schedule/full", Schedule.URI_FULL_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "schedule/tsg", Schedule.URI_TSG_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "lessons", Lessons.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "lessons/#", Lessons.ID_URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "marks", Marks.URI_CODE);
        mMatcher.addURI(Contract.AUTHORITY, "marks/#", Marks.ID_URI_CODE);
    }

    private static final SparseArray<String> mReadTables;
    static {
        mReadTables = new SparseArray<String>();

        mReadTables.append(Events.URI_CODE, Events.TABLE);
        mReadTables.append(Events.ID_URI_CODE, Events.TABLE);
        mReadTables.append(Groups.URI_CODE, Groups.TABLE);
        mReadTables.append(Groups.ID_URI_CODE, Groups.TABLE);
        mReadTables.append(Students.URI_CODE, Students.TABLE);
        mReadTables.append(Students.ID_URI_CODE, Students.TABLE);
        mReadTables.append(Students.URI_BY_GROUP_CODE, new SQLJoinBuilder(Students.TABLE)
                .join(Users.TABLE).onEq(Users._ID, Students.FIELD_USER_ID)
                .join(StudentsGroups.TABLE).onEq(StudentsGroups.FIELD_STUDENT_ID, Students._ID)
                .create());
        mReadTables.append(Students.URI_AS_USERS_CODE, new SQLJoinBuilder(Users.TABLE)
                .join(Students.TABLE).onEq(Users._ID, Students.FIELD_USER_ID)
                .create());
        mReadTables.append(Users.URI_CODE, Users.TABLE);
        mReadTables.append(Users.ID_URI_CODE, Users.TABLE);
        mReadTables.append(StudentsGroups.URI_CODE, StudentsGroups.TABLE);
        mReadTables.append(StudentsGroups.ID_URI_CODE, StudentsGroups.TABLE);
        mReadTables.append(Subjects.URI_CODE, Subjects.TABLE);
        mReadTables.append(Subjects.ID_URI_CODE, Subjects.TABLE);
        mReadTables.append(Teachers.URI_CODE, Teachers.TABLE);
        mReadTables.append(Teachers.URI_AS_USERS_CODE, Teachers.TABLE_JOIN_USERS);
        mReadTables.append(Teachers.URI_WITH_TSG_CODE, Teachers.TABLE_JOIN_TSG);
        mReadTables.append(TeachersSubjects.URI_CODE, TeachersSubjects.TABLE_JOIN_SUBJECTS);
        mReadTables.append(TeachersSubjects.URI_WITH_SUBJECTS_CODE, TeachersSubjects.TABLE_JOIN_SUBJECTS);
        mReadTables.append(TSG.URI_CODE, TSG.TABLE);
        mReadTables.append(TSG.URI_WITH_GROUPS_CODE, TSG.TABLE_JOIN_GROUPS);
        mReadTables.append(TSG.URI_WITH_TEACHERS_CODE, TSG.TABLE_JOIN_TEACHERS_USERS);
        mReadTables.append(TSG.URI_WITH_SUBJECTS_CODE, TSG.TABLE_JOIN_SUBJECTS);
        mReadTables.append(Schedule.URI_CODE, Schedule.TABLE);
        mReadTables.append(Schedule.URI_FULL_CODE, Schedule.TABLE_JOIN_FULL);
        mReadTables.append(Schedule.URI_TSG_CODE, Schedule.TABLE_JOIN_TSG);
        mReadTables.append(Lessons.URI_CODE, Lessons.TABLE);
        mReadTables.append(Marks.URI_CODE, Marks.TABLE);
    }

    private static final SparseArray<String> mWriteTables;
    static {
        mWriteTables = new SparseArray<String>();

        mWriteTables.append(Events.URI_CODE, Events.TABLE);
        mWriteTables.append(Events.ID_URI_CODE, Events.TABLE);
        mWriteTables.append(Groups.URI_CODE, Groups.TABLE);
        mWriteTables.append(Groups.ID_URI_CODE, Groups.TABLE);
        mWriteTables.append(Students.URI_CODE, Students.TABLE);
        mWriteTables.append(Students.ID_URI_CODE, Students.TABLE);
        mWriteTables.append(Users.URI_CODE, Users.TABLE);
        mWriteTables.append(Users.ID_URI_CODE, Users.TABLE);
        mWriteTables.append(StudentsGroups.URI_CODE, StudentsGroups.TABLE);
        mWriteTables.append(StudentsGroups.ID_URI_CODE, StudentsGroups.TABLE);
        mWriteTables.append(Subjects.URI_CODE, Subjects.TABLE);
        mWriteTables.append(Subjects.ID_URI_CODE, Subjects.TABLE);
        mWriteTables.append(Teachers.URI_CODE, Teachers.TABLE);
        mWriteTables.append(TeachersSubjects.URI_CODE, TeachersSubjects.TABLE);
        mWriteTables.append(TSG.URI_CODE, TSG.TABLE);
        mWriteTables.append(Schedule.URI_CODE, Schedule.TABLE);
        mWriteTables.append(Lessons.URI_CODE, Lessons.TABLE);
        mWriteTables.append(Lessons.ID_URI_CODE, Lessons.TABLE);
        mWriteTables.append(Marks.URI_CODE, Marks.TABLE);
        mWriteTables.append(Marks.ID_URI_CODE, Marks.TABLE);
    }

    private static final SparseArray<String> mPrimaryColumns;
    static {
        mPrimaryColumns = new SparseArray<String>();
        mPrimaryColumns.append(Events.ID_URI_CODE, Events._ID);
        mPrimaryColumns.append(Groups.ID_URI_CODE, Groups._ID);
        mPrimaryColumns.append(Students.ID_URI_CODE, Students._ID);
        mPrimaryColumns.append(Users.ID_URI_CODE, Users._ID);
        mPrimaryColumns.append(StudentsGroups.ID_URI_CODE, StudentsGroups._ID);
        mPrimaryColumns.append(Subjects.ID_URI_CODE, Subjects._ID);
        mPrimaryColumns.append(Teachers.ID_URI_CODE, Teachers._ID);
        mPrimaryColumns.append(TeachersSubjects.ID_URI_CODE, TeachersSubjects._ID);
        mPrimaryColumns.append(TSG.ID_URI_CODE, TSG._ID);
        mPrimaryColumns.append(Schedule.ID_URI_CODE, Schedule._ID);
        mPrimaryColumns.append(Marks.ID_URI_CODE, Marks._ID);
        mPrimaryColumns.append(Lessons.ID_URI_CODE, Lessons._ID);

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
            case Events.URI_CODE:
                return Events.CONTENT_TYPE;
            case Events.ID_URI_CODE:
                return Events.CONTENT_ITEM_TYPE;

            case Groups.URI_CODE:
                return Groups.CONTENT_TYPE;
            case Groups.ID_URI_CODE:
                return Groups.CONTENT_ITEM_TYPE;

            case Students.ID_URI_CODE:
            case Students.ID_URI_BY_GROUP_CODE:
                return Students.CONTENT_ITEM_TYPE;
            case Students.URI_BY_GROUP_CODE:
                return Students.CONTENT_TYPE;

            case Users.URI_CODE:
                return Users.CONTENT_TYPE;
            case Users.ID_URI_CODE:
                return Users.CONTENT_ITEM_TYPE;

            case StudentsGroups.URI_CODE:
                return StudentsGroups.CONTENT_TYPE;
            case StudentsGroups.ID_URI_CODE:
                return StudentsGroups.CONTENT_ITEM_TYPE;
        }
        return null;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = getDBHelper().getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
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
