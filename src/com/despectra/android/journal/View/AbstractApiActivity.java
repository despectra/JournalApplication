package com.despectra.android.journal.view;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.ApiServiceHelper;

/**
 * Created by Dmitry on 28.03.14.
 */
public abstract class AbstractApiActivity extends FragmentActivity implements ApiServiceHelper.ApiClient {
    protected JournalApplication mApplicationContext;
    protected ApiServiceHelper.Controller mServiceHelperController;
    protected ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        mApplicationContext = (JournalApplication) getApplicationContext();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONCREATE);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(prepareView(layoutResID));
    }

    private View prepareView(int layoutResID) {
        View rootView = getLayoutInflater().inflate(R.layout.activity_abstract, null);
        View childView = getLayoutInflater().inflate(layoutResID, null);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.activity_progress);
        ((FrameLayout)rootView.findViewById(R.id.activity_container)).addView(childView);
        return rootView;
    }

    public void showProgressBar() {
        //mProgressBar.setVisibility(View.VISIBLE);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_INDETERMINATE_ON);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
    }

    public void hideProgressBar() {
        //mProgressBar.setVisibility(View.INVISIBLE);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_INDETERMINATE_OFF);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_OFF);
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
