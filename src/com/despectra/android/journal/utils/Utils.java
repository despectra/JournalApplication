package com.despectra.android.journal.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import com.despectra.android.journal.logic.local.DBHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by Dmitry on 25.03.14.
 */
public class Utils {
    public static int[] HOLO_COLORS = {android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_green_dark,
    android.R.color.holo_blue_dark,
    android.R.color.holo_orange_light,
    android.R.color.holo_purple,
    android.R.color.holo_red_light};

    public static void setViewWidth(View v, int width) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = width;
        v.setLayoutParams(lp);
    }

    public static int getRandomHoloColor() {
        Random r = new Random(System.currentTimeMillis());
        return HOLO_COLORS[r.nextInt(HOLO_COLORS.length)];
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static int getAbsListViewOffset(AbsListView listView) {
        int offset = -1;
        final View first = listView.getChildAt(0);
        if (first != null) {
            offset += first.getTop();
        }
        return offset;
    }

    public static Point getDisplayDimension(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static int getScreenCategory(Context context) {
        return context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
    }

    public static boolean isApiJsonSuccess(JSONObject jsonResponse) {
        try {
            return jsonResponse.has("success") && jsonResponse.getInt("success") == 1;
        } catch (JSONException e) {
            return false;
        }
    }

    public static String md5(final String toEncrypt) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(toEncrypt.getBytes());
            final byte[] bytes = digest.digest();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02X", bytes[i]));
            }
            return sb.toString().toLowerCase();
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
    }

    public static void clearLocalDB(SQLiteDatabase db) {
        db.execSQL(String.format(DBHelper.DROP_TABLE, "events"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "groups"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "students"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "users"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "students_groups"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "subjects"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "lessons"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "marks"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "teachers"));
        db.execSQL(String.format(DBHelper.DROP_TABLE, "teachers_subjects"));
        db.execSQL(DBHelper.CREATE_TABLE_EVENTS);
        db.execSQL(DBHelper.CREATE_TABLE_GROUPS);
        db.execSQL(DBHelper.CREATE_TABLE_STUDENTS);
        db.execSQL(DBHelper.CREATE_TABLE_USERS);
        db.execSQL(DBHelper.CREATE_TABLE_STUDENTS_GROUPS);
        db.execSQL(DBHelper.CREATE_TABLE_SUBJECTS);
        db.execSQL(DBHelper.CREATE_TABLE_TEACHERS);
        db.execSQL(DBHelper.CREATE_TABLE_TEACHERS_SUBJECTS);
    }

    public static long[] getIdsFromJSONArray(JSONArray array) throws JSONException {
        long[] ids = new long[array.length()];
        for (int i = 0; i < array.length(); i++) {
            ids[i] = array.getLong(i);
        }
        return ids;
    }

    public static int dpToPx(Context context, int dp) {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void setViewHeight(View v, int height) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.height = height;
        v.setLayoutParams(lp);
    }
}
