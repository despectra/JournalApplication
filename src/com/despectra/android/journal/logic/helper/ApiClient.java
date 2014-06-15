package com.despectra.android.journal.logic.helper;

/**
 * Created by Андрей on 12.06.14.
 */
public interface ApiClient extends ApiServiceHelper.Callback {
    void setServiceHelperController(HelperController controller);
    String getClientName();
}
