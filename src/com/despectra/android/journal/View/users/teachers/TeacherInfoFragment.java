package com.despectra.android.journal.view.users.teachers;

import android.database.Cursor;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.view.users.AbstractUserFragment;

/**
 * Created by Dmitry on 04.06.14.
 */
public class TeacherInfoFragment extends AbstractUserFragment {

    public TeacherInfoFragment() {
        super();
    }

    @Override
    protected void updateUserInfo() {
        mServiceHelperController.getTeacher(mToken,
                mUserIds.getIdsByTable("users"),
                mUserIds.getIdsByTable("teachers"),
                ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        super.onLoadFinished(cursorLoader, cursor);
        ((ActionBarActivity)getActivity())
                .getSupportActionBar()
                .setSubtitle(String.format("%s %s %s", mSecondName, mFirstName, mMiddleName));
    }

}
