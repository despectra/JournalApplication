package com.despectra.android.journal.view.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.widget.Toast;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.view.AbstractApiActionBarActivity;
import com.despectra.android.journal.view.SimpleProgressDialog;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import org.json.JSONObject;

import java.util.regex.Pattern;

/**
 * Created by Dmitry on 25.03.14.
 */
public class PreferencesActivity extends AbstractApiActionBarActivity implements ApiServiceHelper.Callback {
    public static final String KEY_CHEKING_STATE = "checking";

    public static final String PROGRESS_DIALOG_TAG = "checkingDialog";

    private MainPreferencesFragment mFragment;
    private SimpleProgressDialog mCheckingDialog;
    private boolean mIsChecking;
    private String mCheckedHost;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayUseLogoEnabled(false);
        mCheckingDialog = (SimpleProgressDialog) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
        if (mCheckingDialog == null) {
            mCheckingDialog = SimpleProgressDialog.newInstance("Проверка сервера...");
            mCheckingDialog.setCancelable(false);
        }
        if (savedInstanceState != null) {
            mFragment = (MainPreferencesFragment)getFragmentManager().findFragmentByTag(MainPreferencesFragment.FRAGMENT_TAG);
            boolean checking = savedInstanceState.getBoolean(KEY_CHEKING_STATE);
            setCheckingState(checking);
        } else {
            mFragment = new MainPreferencesFragment();
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, mFragment, MainPreferencesFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CHEKING_STATE, mIsChecking);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected boolean showUpButton() {
        return true;
    }

    public void setCheckingState(boolean checking) {
        mIsChecking = checking;
        if (mIsChecking) {
            if (!mCheckingDialog.isAdded()) {
                mCheckingDialog.show(getSupportFragmentManager(), PROGRESS_DIALOG_TAG);
            }
        } else if(mCheckingDialog.isAdded()) {
            mCheckingDialog.dismiss();
        }
    }

    public void checkHost(String host) {
        boolean hasHttp = Pattern.matches("^http://.*$", host);
        if (!hasHttp) {
            host = "http://" + host;
        }
        boolean isCorrectHost = Pattern.matches("^http://[a-z0-9.-/_]*$", host);
        if (!isCorrectHost) {
            Toast.makeText(this, "Адрес сервера введен неправильно", Toast.LENGTH_SHORT).show();
            return;
        }
        mCheckedHost = host;
        setCheckingState(true);
        mServiceHelperController.getApiInfo(mCheckedHost, ApiServiceHelper.PRIORITY_LOW);
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        if (actionCode == APICodes.ACTION_GET_INFO) {
            mFragment.updateHost(mCheckedHost);
            setCheckingState(false);
        }
    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        ApiErrorResponder.respondDialog(getSupportFragmentManager(), (JSONObject)response);
        setCheckingState(false);
    }

    public static class MainPreferencesFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        public static final String FRAGMENT_TAG = "prefFragment";

        public MainPreferencesFragment() {
            super();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            updateSummaries(getPreferenceScreen());
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            String token = prefs.getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
            findPreference(JournalApplication.PREFERENCE_KEY_HOST).setEnabled(token.isEmpty());
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            updateSummaries(getPreferenceScreen());
        }

        private void updateSummaries(Preference item) {
            if (item instanceof PreferenceGroup) {
                PreferenceGroup group = (PreferenceGroup) item;
                for (int i = 0; i < group.getPreferenceCount(); i++) {
                    Preference groupItem = group.getPreference(i);
                    if (groupItem instanceof PreferenceGroup) {
                        updateSummaries(groupItem);
                    } else if (groupItem instanceof EditTextPreference || groupItem instanceof ListPreference) {
                        updateSimpleSummary(groupItem);
                    }
                }
            }
        }

        private void updateSimpleSummary(Preference item) {
            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
            item.setSummary(sharedPreferences.getString(item.getKey(), ""));
        }

        public void updateHost(String host) {
            getPreferenceScreen().getSharedPreferences()
                    .edit()
                    .putString(JournalApplication.PREFERENCE_KEY_HOST, host)
                    .commit();
            updateSummaries(getPreferenceScreen());
        }
    }
}
