package com.despectra.android.journal.Data;

import android.net.Uri;
import android.provider.BaseColumns;
import com.despectra.android.journal.App.JournalApplication;

/**
 * Created by Dmitry on 10.04.2014.
 */
public final class Contract {
    //base
    public static final String AUTHORITY = "com.despectra.android.journal.provider";
    public static final String STRING_URI = "content://" + AUTHORITY;

    public static final String FIELD_ID = BaseColumns._ID;
    public static final String FIELD_REMOTE_ID = "remote_id";
    public static final String FIELD_ENTITY_STATUS = "entity_status";

    //entity statuses
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_INSERTING = 1;
    public static final int STATUS_UPDATING = 2;
    public static final int STATUS_DELETING = 3;

    public static final String DIR_VND = "vnd.android.cursor.dir/vnd.";
    public static final String ITEM_VND = "vnd.android.cursor.item/vnd.";

    public static final class Events {
        public static final String TABLE = "events";
        public static final String FIELD_TEXT = "text";
        public static final String FIELD_DATETIME = "datetime";
        public static final Uri URI = Uri.parse(STRING_URI + "/" + TABLE);
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 1;
        public static final int ID_URI_CODE = 2;
    }

    public static final class Groups {
        public static final String TABLE = "groups";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_PARENT_ID = "parent_id";
        public static final Uri URI = Uri.parse(STRING_URI + "/" + TABLE);
        public static final String CONTENT_TYPE = DIR_VND + AUTHORITY + "." + TABLE;
        public static final String CONTENT_ITEM_TYPE = ITEM_VND + AUTHORITY + "." + TABLE;
        public static final int URI_CODE = 3;
        public static final int ID_URI_CODE = 4;
    }
}
