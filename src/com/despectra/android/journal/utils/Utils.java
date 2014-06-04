package com.despectra.android.journal.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Dmitry on 25.03.14.
 */
public class Utils {
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
}
