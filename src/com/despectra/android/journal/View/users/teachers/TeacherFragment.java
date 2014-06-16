package com.despectra.android.journal.view.users.teachers;

import android.support.v4.app.Fragment;
import com.despectra.android.journal.view.PagerContainerFragment;
import com.despectra.android.journal.view.subjects.SubjectsOfTeacherFragment;

/**
 * Created by Dmitry on 03.06.14.
 */
public class TeacherFragment extends PagerContainerFragment {

    public TeacherFragment() {
        super();
    }

    @Override
    public Fragment getPagerItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new TeacherInfoFragment();
                fragment.setArguments(getArguments());
                return fragment;
            case 1:
                //TODO create other fragment
                fragment = new SubjectsOfTeacherFragment();
                fragment.setArguments(getArguments());
                return fragment;
            case 2:
            default:
                fragment = new Fragment();
        }
        return fragment;
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
