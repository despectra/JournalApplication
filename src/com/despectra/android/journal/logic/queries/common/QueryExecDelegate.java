package com.despectra.android.journal.logic.queries.common;

import android.content.Context;
import android.os.Handler;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.net.ApplicationServer;

/**
 * Created by Dmitry on 02.06.14.
 */
public abstract class QueryExecDelegate implements DelegatingInterface {
    private DelegatingInterface mHolderInterface;

    public QueryExecDelegate(DelegatingInterface holderInterface) {
        mHolderInterface = holderInterface;
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
    public LocalStorageManager getLocalStorageManager() {
        return mHolderInterface.getLocalStorageManager();
    }

    @Override
    public Handler getResponseHandler() {
        return mHolderInterface.getResponseHandler();
    }
}
