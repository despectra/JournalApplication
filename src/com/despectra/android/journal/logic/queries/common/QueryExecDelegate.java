package com.despectra.android.journal.logic.queries.common;

import android.content.Context;
import android.os.Handler;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.net.ApplicationServer;

import java.util.Map;

/**
 * Created by Dmitry on 02.06.14.
 */
public abstract class QueryExecDelegate implements DelegatingInterface {
    protected EntityTable mTable;
    private DelegatingInterface mHolderInterface;
    private LocalStorageManager mLSManager;

    public QueryExecDelegate(DelegatingInterface holderInterface, Map<String, Object> configs) {
        mHolderInterface = holderInterface;
        mLSManager = new LocalStorageManager(mHolderInterface.getContext());
        if (configs != null && configs.containsKey("LSM_CALLBACKS")) {
            mLSManager.setCallbacks((LocalStorageManager.PostCallbacks) configs.get("LSM_CALLBACKS"));
        }
    }

    @Override
    public Context getContext() {
        return mHolderInterface.getContext();
    }

    @Override
    public ApplicationServer getApplicationServer() {
        return mHolderInterface.getApplicationServer();
    }

    @Override
    public Handler getResponseHandler() {
        return mHolderInterface.getResponseHandler();
    }

    public LocalStorageManager getLocalStorageManager() {
        return mLSManager;
    }
}
