package com.despectra.android.journal.view.users;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.view.PagerContainerFragment;

/**
 * Created by Dmitry on 03.06.14.
 */
public class TeacherFragment extends PagerContainerFragment {

    @Override
    public Fragment getPagerItem(int position) {
        switch (position) {
            case 0:
                AbstractUserFragment fragment =  new AbstractUserFragment() {
                    @Override
                    protected void updateUserInfo() {
                        mServiceHelperController.getTeacher(mToken,
                                mUserIds.getIdsByTable("users"),
                                mUserIds.getIdsByTable("teachers"),
                                ApiServiceHelper.PRIORITY_HIGH);
                    }
                };
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
