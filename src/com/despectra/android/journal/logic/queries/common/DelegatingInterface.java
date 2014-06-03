package com.despectra.android.journal.logic.queries.common;

import android.content.Context;
import android.os.Handler;
import com.despectra.android.journal.logic.local.LocalStorageManager;
import com.despectra.android.journal.logic.net.ApplicationServer;

/**
 * Created by Dmitry on 02.06.14.
 */
public interface DelegatingInterface {
    public Context getContext();
    public ApplicationServer getApplicationServer();
    public LocalStorageManager getLocalStorageManager();
    public Handler getResponseHandler();
}
