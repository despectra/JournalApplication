package com.despectra.android.journal.Activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Services.ApiServiceHelper;

/**
 * Created by Dmitry on 28.03.14.
 */
public abstract class AbstractApiActivity extends FragmentActivity implements ApiServiceHelper.Callback, ApiServiceHelper.ApiClient {
    JournalApplication mApplicationContext;
    ApiServiceHelper.Controller mServiceHelperController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationContext = (JournalApplication) getApplicationContext();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONCREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONSTART);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONRESUME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONPAUSE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONSTOP);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONDESTROY);
    }

    @Override
    public void setServiceHelperController(ApiServiceHelper.Controller controller) {
        mServiceHelperController = controller;
    }

    @Override
    public String getClientName() {
        return getClass().getSimpleName();
    }
}
