package com.despectra.android.journal.logic.helper;

/**
 * Created by Андрей on 12.06.14.
 */
public interface ApiClientWithProgress extends ApiClient {
    void showProgress();
    void hideProgress();
}
