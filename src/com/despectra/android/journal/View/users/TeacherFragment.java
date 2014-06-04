package com.despectra.android.journal.view.users;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.view.PagerContainerFragment;

/**
 * Created by Dmitry on 03.06.14.
 */
public class TeacherFragment extends PagerContainerFragment {

    public TeacherFragment() {
        super();
    }

    @Override
    public Fragment getPagerItem(int position) {
        switch (position) {
            case 0:
                AbstractUserFragment fragment = new TeacherInfoFragment();
                fragment.setArguments(getArguments());
                return fragment;
            case 1:
            case 2:
            default:
                return new Fragment();
        }
    }

    @Override
    public int getPagerItemsCount() {
        return 3;
    }

    @Override
    public String getPagerItemTitle(int position) {
        switch (position) {
            case 0:
                return "Общая информация";
            case 1:
                return "Предметы";
            case 2:
                return "Права";
            default:
                return "";
        }
    }
}
