package com.despectra.android.journal.logic.queries.common;

import android.content.Context;
import android.os.Handler;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.net.ApplicationServer;
import com.despectra.android.journal.logic.queries.*;

import java.util.Map;

/**
 * Created by Dmitry on 18.05.14.
 */
public class QueryExecutorImpl implements QueryExecutor, DelegatingInterface {

    private Context mContext;
    private ApplicationServer mServer;
    private Handler mResponseHandler;

    public QueryExecutorImpl(Context context, ApplicationServer server, Handler responseHandler) {
        mContext = context;
        mServer = server;
        mResponseHandler = responseHandler;
    }

    @Override
    public Events forEvents(Map<String, Object> configs) {
        return new Events(this, configs);
    }

    @Override
    public Groups forGroups(Map<String, Object> configs) {
        return new Groups(this, configs);
    }

    @Override
    public Students forStudents(Map<String, Object> configs) {
        return new Students(this, configs);
    }

    @Override
    public Subjects forSubjects(Map<String, Object> configs) {
        return new Subjects(this, configs);
    }

    @Override
    public Teachers forTeachers(Map<String, Object> configs) {
        return new Teachers(this, configs);
    }

    @Override
    public Schedule forSchedule(Map<String, Object> configs) {
       return new Schedule(this, configs);
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
    public Handler getResponseHandler() {
        return mResponseHandler;
    }
}
