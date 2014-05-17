package com.despectra.android.journal.Utils;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;

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
}
