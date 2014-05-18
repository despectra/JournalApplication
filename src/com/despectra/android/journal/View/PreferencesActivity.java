package com.despectra.android.journal.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.widget.Toast;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Dialogs.SimpleProgressDialog;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Services.ApiServiceHelper;
import com.despectra.android.journal.view.AbstractApiActivity;
import org.json.JSONObject;

import java.util.regex.Pattern;

/**
 * Created by Dmitry on 25.03.14.
 */
public class PreferencesActivity extends AbstractApiActivity implements ApiServiceHelper.Callback {
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
    public void onResponse(int actionCode, int remainingActions, Object response) {
        if (actionCode != -1) {
            if (actionCode == APICodes.ACTION_GET_INFO) {
                try {
                    JSONObject data = (JSONObject) response;
                    boolean ok = data.getBoolean("ok");
                    if (ok) {
                        mFragment.updateHost(mCheckedHost);
                    }
                    setCheckingState(false);
                } catch (Exception ex) {
                    Toast.makeText(this, String.format("Сервер %s недоступен", mCheckedHost), Toast.LENGTH_SHORT).show();
                    setCheckingState(false);
                }
            }
        } else {
            Toast.makeText(this, String.format("Ошибка при обращении к серверу: %s", response), Toast.LENGTH_SHORT).show();
            setCheckingState(false);
        }
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
