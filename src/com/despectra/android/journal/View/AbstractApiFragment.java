package com.despectra.android.journal.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONObject;

/**
 * Created by Dmitry on 07.04.14.
 */
public abstract class AbstractApiFragment extends Fragment implements ApiServiceHelper.ApiClient, IActivityProgressBar {
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
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONDESTROY);
        super.onDestroy();
    }

    @Override
    public void setServiceHelperController(ApiServiceHelper.Controller controller) {
        mServiceHelperController = controller;
    }

    @Override
    public void showProgress() {
        if (getActivity() instanceof IActivityProgressBar) {
            ((IActivityProgressBar)getActivity()).showProgress();
        }
    }

    @Override
    public void hideProgress() {
        if (getActivity() instanceof IActivityProgressBar) {
            ((IActivityProgressBar)getActivity()).hideProgress();
        }
    }

    @Override
    public String getClientName() {
        return getClass().getSimpleName();
    }


    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {
        hideProgress();
        JSONObject jsonResponse = (JSONObject) response;
        if (Utils.isApiJsonSuccess(jsonResponse)) {
            onResponseSuccess(actionCode, remainingActions, response);
        } else {
            onResponseError(actionCode, remainingActions, response);
        }
    }

    protected abstract void onResponseSuccess(int actionCode, int remainingActions, Object response);

    protected abstract void onResponseError(int actionCode, int remainingActions, Object response);
}
