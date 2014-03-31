package com.despectra.android.journal.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Dialogs.SimpleInfoDialog;
import com.despectra.android.journal.Dialogs.SimpleProgressDialog;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Services.ApiServiceHelper;
import com.despectra.android.journal.Utils.Utils;
import org.json.JSONObject;

/**
 * Created by Dmitry on 25.03.14.
 */
public class LoginActivity extends ApiActivity implements TextView.OnEditorActionListener, ApiServiceHelper.Callback {
    private static final String KEY_STATUS = "isLogging";

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
    private SimpleInfoDialog mErrorDialog;

    private int mLoggingStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        if (!token.equals("")) {
            launchMainActivity();
        } else {
            initUi(savedInstanceState);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mServiceHelperController = mApplicationContext.getApiServiceHelper().registerActivity(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterActivity(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

        mLoggingDialog = (SimpleProgressDialog) getFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
        if (mLoggingDialog == null) {
            mLoggingDialog = SimpleProgressDialog.newInstance("Вход..");
            mLoggingDialog.setCancelable(false);
        }
        if (savedState != null) {
            mLoggingStatus = savedState.getInt(KEY_STATUS);
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
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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
        mLoggingDialog.show(getFragmentManager(), PROGRESS_DIALOG_TAG);
        mServiceHelperController.login(login, pass);
    }

    @Override
    public void onResponse(int actionCode, Object response) {
        if (actionCode != -1) {
            JSONObject jsonData = (JSONObject)response;
            try {
                switch (actionCode) {
                    case APICodes.ACTION_LOGIN:
                        int success = jsonData.getInt("success");
                        if (success == 0) {
                            onLoggingError(jsonData.getString("error_message"));
                            return;
                        }
                        String token = jsonData.getString("token");
                        PreferenceManager.getDefaultSharedPreferences(this)
                                .edit()
                                .putString(JournalApplication.PREFERENCE_KEY_TOKEN, token)
                                .commit();
                        mLoggingStatus = STATUS_RETRIEVING_DATA;
                        updateProgressDialogMessage();
                        mServiceHelperController.getMinProfile(token);
                        break;
                    case APICodes.ACTION_GET_MIN_PROFILE:
                        if (jsonData.has("success")) {
                            onLoggingError(jsonData.getString("error_message"));
                            return;
                        }
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
        } else {
            onLoggingError("Ошибка при входе");
        }
    }

    private void onLoggingError(String errorMsg) {
        mLoggingStatus = STATUS_IDLE;
        mLoggingDialog.dismiss();
        if (mErrorDialog == null) {
            mErrorDialog = SimpleInfoDialog.newInstance("Ошибка", errorMsg);
            mErrorDialog.show(getFragmentManager(), ERROR_DIALOG_TAG);
        }
    }
}
