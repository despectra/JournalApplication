package com.despectra.android.journal.view;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.logic.helper.ApiClientWithProgress;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.helper.BasicClientHelperController;
import com.despectra.android.journal.logic.helper.HelperController;
import com.despectra.android.journal.utils.Utils;
import org.json.JSONObject;

/**
 * Created by Dmitry on 07.04.14.
 */
public abstract class AbstractApiFragment extends Fragment implements ApiClientWithProgress {
    protected JournalApplication mApplicationContext;
    protected HelperController mServiceHelperController;
    protected Cursor mCursor;
    protected String mToken;
    protected boolean mLoading;

    protected AbstractApiFragment() {
        super();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mApplicationContext = (JournalApplication) getActivity().getApplicationContext();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONCREATE);
        mToken = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        mLoading = savedInstanceState != null && savedInstanceState.getBoolean("loading");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("loading", mLoading);
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
        mApplicationContext.getApiServiceHelper().registerClient(this,
                new BasicClientHelperController(getActivity().getApplicationContext(), getClientName()));
    }

    @Override
    public void onPause() {
        super.onPause();
        hideProgress();
        mApplicationContext.lifecycleStateChanged(getClass().getSimpleName(), JournalApplication.ONPAUSE);
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
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
    public void setServiceHelperController(HelperController controller) {
        mServiceHelperController = controller;
    }

    @Override
    public void showProgress() {
        if (getActivity() instanceof ApiClientWithProgress) {
            ((ApiClientWithProgress)getActivity()).showProgress();
        }
    }

    @Override
    public void hideProgress() {
        if (getActivity() instanceof ApiClientWithProgress) {
            ((ApiClientWithProgress)getActivity()).hideProgress();
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
