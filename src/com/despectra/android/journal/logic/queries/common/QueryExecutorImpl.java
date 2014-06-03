package com.despectra.android.journal.logic.queries.common;

import android.content.Context;
import android.os.Handler;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.net.ApplicationServer;
import com.despectra.android.journal.logic.queries.*;

/**
 * Created by Dmitry on 18.05.14.
 */
public class QueryExecutorImpl implements QueryExecutor, DelegatingInterface {

    private Context mContext;
    private ApplicationServer mServer;
    private LocalStorageManager mLocalStorageManager;
    private Handler mResponseHandler;

    public QueryExecutorImpl(Context context, ApplicationServer server, Handler responseHandler) {
        mContext = context;
        mServer = server;
        mLocalStorageManager = new LocalStorageManager(context, Contract.STRING_URI);
        mResponseHandler = responseHandler;
    }

    @Override
    public Events forEvents() {
        return new Events(this);
    }

    @Override
    public Groups forGroups() {
        return new Groups(this);
    }

    @Override
    public Students forStudents() {
        return new Students(this);
    }

    @Override
    public Subjects forSubjects() {
        return new Subjects(this);
    }

    @Override
    public Teachers forTeachers() {
        return new Teachers(this);
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public ApplicationServer getApplicationServer() {
        return mServer;
    }

    @Override
    public LocalStorageManager getLocalStorageManager() {
        return mLocalStorageManager;
    }

    @Override
    public Handler getResponseHandler() {
        return mResponseHandler;
    }
}
