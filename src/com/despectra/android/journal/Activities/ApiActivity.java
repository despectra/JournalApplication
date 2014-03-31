package com.despectra.android.journal.Activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Services.ApiServiceHelper;

/**
 * Created by Dmitry on 28.03.14.
 */
public abstract class ApiActivity extends FragmentActivity {
    JournalApplication mApplicationContext;
    ApiServiceHelper.Controller mServiceHelperController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationContext = (JournalApplication) getApplicationContext();
        mApplicationContext.activityStateChanged(getClass().getSimpleName(), JournalApplication.ONCREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApplicationContext.activityStateChanged(getClass().getSimpleName(), JournalApplication.ONSTART);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApplicationContext.activityStateChanged(getClass().getSimpleName(), JournalApplication.ONRESUME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApplicationContext.activityStateChanged(getClass().getSimpleName(), JournalApplication.ONPAUSE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mApplicationContext.activityStateChanged(getClass().getSimpleName(), JournalApplication.ONSTOP);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mApplicationContext.activityStateChanged(getClass().getSimpleName(), JournalApplication.ONDESTROY);
    }
}
