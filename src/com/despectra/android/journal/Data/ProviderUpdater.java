package com.despectra.android.journal.Data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Dmitry on 01.04.14.
 */
public class ProviderUpdater {

    // insert new, update existing
    public static final int MODE_APPEND = 0;
    //insert new, update existing, delete nonexistent
    public static final int MODE_REPLACE = 1;

    private Context mContext;
    private String mProviderUri;

    public ProviderUpdater(Context context, String providerUri) {
        mContext = context;
        mProviderUri = providerUri;
    }

    public void updateTableWithRows(String table, ContentValues[] rows) {

    }

    public void updateTableWithJSONArray(String table,
                                         JSONArray json,
                                         String[] from,
                                         String[] to,
                                         String primaryInJson,
                                         String primaryInTable) throws JSONException {
        ContentResolver resolver = mContext.getContentResolver();
        String tableUri = mProviderUri + "/" + table;
        for (int i = 0; i < json.length(); i++) {
            JSONObject element = json.getJSONObject(i);
            int id = element.getInt(primaryInJson);
            String checkingUri = String.format("%s/%d", tableUri, id);
            Cursor lookUpRow = resolver.query(Uri.parse(checkingUri), new String[]{primaryInTable}, null, null, null);
            int count = lookUpRow.getCount();
            if (count >= 0) {
                ContentValues row = new ContentValues();
                for (int j = 0; j < from.length; j++) {
                    try {
                        row.put(to[j], element.getString(from[j]));
                    } catch(JSONException ex1) {
                        try {
                            row.put(to[j], element.getInt(from[j]));
                        } catch (JSONException ex2) {
                            try {
                                row.put(to[j], element.getLong(from[j]));
                            } catch (JSONException ex3) {
                                //nothing here.. SUppose, it's unreachable
                            }
                        }
                    }
                }

                if (count == 0) {
                    //insert new
                    resolver.insert(Uri.parse(tableUri), row);
                    ;
                } else if (count == 1) {
                    //update existing
                    resolver.update(Uri.parse(checkingUri), row, null, null);
                    ;
                }
            }

        }
    }
}
