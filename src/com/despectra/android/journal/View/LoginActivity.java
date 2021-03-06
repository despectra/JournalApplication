package com.despectra.android.journal.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.BasicClientHelperController;
import com.despectra.android.journal.logic.net.APICodes;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.preferences.PreferencesActivity;
import org.json.JSONObject;

/**
 * Created by Dmitry on 25.03.14.
 */
public class LoginActivity extends AbstractApiFragmentActivity implements TextView.OnEditorActionListener, ApiServiceHelper.Callback {
    private static final String KEY_STATUS = "isLogging";
    public static final String KEY_LOGIN = "login";

    public static final String PROGRESS_DIALOG_TAG = "progressDialog";
    public static final String ERROR_DIALOG_TAG = "errorDialog";

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_LOGGING = 1;
    public static final int STATUS_RETRIEVING_DATA = 2;

    private Button mLoginBtn;
    private ImageButton mSettingsButton;
    private EditText mLoginEdit;
    private EditText mPassEdit;
    private TextView mResponseText;

    private SimpleProgressDialog mLoggingDialog;

    private int mLoggingStatus;
    private String mLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String token = prefs.getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        int uid = prefs.getInt(JournalApplication.PREFERENCE_KEY_UID, 0);
        if (!token.equals("") && uid > 0) {
            launchMainActivity();
        } else {
            initUi(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LOGIN, mLogin);
        outState.putInt(KEY_STATUS, mLoggingStatus);
    }

    private void initUi(Bundle savedState) {
        setContentView(R.layout.activity_login);
        mLoginEdit = (EditText)findViewById(R.id.login_edit);
        mLoginEdit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mPassEdit = (EditText)findViewById(R.id.password_edit);
        mPassEdit.setImeActionLabel("Login", KeyEvent.KEYCODE_ENTER);
        mPassEdit.setOnEditorActionListener(this);
        mLoginBtn = (Button)findViewById(R.id.login_btn);
        mResponseText = (TextView)findViewById(R.id.response_text);

        mLoggingDialog = (SimpleProgressDialog) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
        if (mLoggingDialog == null) {
            mLoggingDialog = SimpleProgressDialog.newInstance("Вход..");
            mLoggingDialog.setCancelable(false);
        }
        if (savedState != null) {
            mLoggingStatus = savedState.getInt(KEY_STATUS);
            mLogin = savedState.getString(KEY_LOGIN);
            updateProgressDialogMessage();
        }

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clickedView) {
                if (!Utils.isOnline(LoginActivity.this)) {
                    Toast.makeText(LoginActivity.this, "You're not connected to Internet", Toast.LENGTH_LONG).show();
                    return;
                }
                performLogin();
            }
        });
        mSettingsButton = (ImageButton) findViewById(R.id.settings_btn);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, PreferencesActivity.class);
                startActivity(intent);
            }
        });
    }

    private void updateProgressDialogMessage() {
        switch (mLoggingStatus) {
            case STATUS_IDLE:
                break;
            case STATUS_LOGGING:
                mLoggingDialog.setMessage("Вход...");
                break;
            case STATUS_RETRIEVING_DATA:
                mLoggingDialog.setMessage("Получение данных пользователя...");
                break;
        }
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if(actionId == EditorInfo.IME_ACTION_DONE) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
            performLogin();
        }
        return true;
    }

    private void performLogin() {
        String login = mLoginEdit.getText().toString();
        String pass = mPassEdit.getText().toString();
        if(login.isEmpty() || pass.isEmpty()) {
            return;
        }
        mResponseText.setText(null);
        mLoggingStatus = STATUS_LOGGING;
        mLoggingDialog.show(getSupportFragmentManager(), PROGRESS_DIALOG_TAG);
        mLogin = login;
        mServiceHelperController.login(login, pass, ApiServiceHelper.PRIORITY_LOW);
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        JSONObject jsonData = (JSONObject)response;
        try {
            switch (actionCode) {
                case APICodes.ACTION_LOGIN:
                    String token = jsonData.getString("token");
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putString(JournalApplication.PREFERENCE_KEY_TOKEN, token)
                            .putString(JournalApplication.PREFERENCE_KEY_LOGIN, mLogin)
                            .commit();
                    mLoggingStatus = STATUS_RETRIEVING_DATA;
                    updateProgressDialogMessage();
                    mServiceHelperController.getMinProfile(token, ApiServiceHelper.PRIORITY_LOW);
                    break;
                case APICodes.ACTION_GET_MIN_PROFILE:
                    int uid = jsonData.getInt("uid");
                    String name = jsonData.getString("name");
                    String middleName = jsonData.getString("middlename");
                    String surname = jsonData.getString("surname");
                    int level = jsonData.getInt("level");
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putInt(JournalApplication.PREFERENCE_KEY_UID, uid)
                            .putString(JournalApplication.PREFERENCE_KEY_NAME, name)
                            .putString(JournalApplication.PREFERENCE_KEY_MIDDLENAME, middleName)
                            .putString(JournalApplication.PREFERENCE_KEY_SURNAME, surname)
                            .putInt(JournalApplication.PREFERENCE_KEY_LEVEL, level)
                            .commit();
                    mLoggingDialog.dismiss();
                    launchMainActivity();
                    break;
            }
        } catch (Exception ex) {
            ;
        }
    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        JSONObject jsonData = (JSONObject)response;
        ApiErrorResponder.respondDialog(getSupportFragmentManager(), jsonData);
        mLoggingDialog.dismiss();
    }
}
