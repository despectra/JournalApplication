package com.despectra.android.journal.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.despectra.android.journal.view.activities.AbstractApiActivity;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.Services.ApiServiceHelper;

/**
 * Created by Dmitry on 07.04.14.
 */
public abstract class AbstractApiFragment extends Fragment implements ApiServiceHelper.ApiClient {
    protected JournalApplication mApplicationContext;
    protected ApiServiceHelper.Controller mServiceHelperController;

    protected AbstractApiFragment() {
        super();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mApplicationContext = (JournalApplication) getActivity().getApplicationContext();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONCREATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONSTART);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONRESUME);
    }

    @Override
    public void onPause() {
        super.onPause();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONPAUSE);
    }

    @Override
    public void onStop() {
        super.onStop();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONSTOP);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONDESTROY);
    }

    @Override
    public void setServiceHelperController(ApiServiceHelper.Controller controller) {
        mServiceHelperController = controller;
    }

    public AbstractApiActivity getHostActivity() {
        return (AbstractApiActivity) getActivity();
    }

    @Override
    public String getClientName() {
        return getClass().getSimpleName();
    }

}
