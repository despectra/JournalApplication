package com.despectra.android.journal.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.Toast;
import com.despectra.android.journal.Activities.PreferencesActivity;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Services.ApiServiceHelper;
import org.json.JSONObject;

import java.util.regex.Pattern;

/**
 * Created by Dmitry on 25.03.14.
 */
public class HostPreferenceDialog extends EditTextPreference {
    private String mCheckedHost;
    private PreferencesActivity mActivity;

    public HostPreferenceDialog(Context context) {
        super(context);
        mActivity = (PreferencesActivity) context;
    }

    public HostPreferenceDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (PreferencesActivity) context;
    }

    public HostPreferenceDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mActivity = (PreferencesActivity) context;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String host = getEditText().getText().toString();
            String currentHost = getSharedPreferences().getString(getKey(), "");
            if (!host.equals(currentHost)){
                mActivity.checkHost(host);
            }
        }
    }
}
