package com.despectra.android.journal.utils;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;
import com.despectra.android.journal.view.SimpleInfoDialog;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Dmitry on 30.05.14.
 */
public class ApiErrorResponder {

    public static int getErrorCode(JSONObject corruptResponse) {
        try {
            return corruptResponse.getInt("error_code");
        } catch (JSONException e) {
            return -1;
        }
    }

    public static String getErrorMessage(JSONObject corruptResponse) {
        try {
            return corruptResponse.getString("error_message");
        } catch (JSONException e) {
            return null;
        }
    }

    public static void respondToast(Context context, JSONObject corruptResponse) {
        String finalMessage = new ErrorExtractor().extractMessage(corruptResponse);
        Toast.makeText(context, finalMessage, Toast.LENGTH_SHORT).show();
    }

    public static void respondDialog(FragmentManager fm, JSONObject corruptResponse) {
        String finalMessage = new ErrorExtractor().extractMessage(corruptResponse);
        SimpleInfoDialog dialog = SimpleInfoDialog.newInstance("Ошибка при исполнении запроса", finalMessage);
        dialog.show(fm, "ErrorDialog");
    }

    private static class ErrorExtractor {
        int code;
        String rawMessage;

        public ErrorExtractor(){

        }

        public String extractMessage(JSONObject corruptJson) {
            String finalMessage;
            try {
                code = corruptJson.getInt("error_code");
                rawMessage = corruptJson.getString("error_message");
                finalMessage = String.format("CODE: %d, MSG: %s", code, rawMessage);
            } catch (JSONException e) {
                finalMessage = "Unknown error";
            }
            return finalMessage;
        }
    }

}
