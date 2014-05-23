package com.despectra.android.journal.view.groups;

import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import com.despectra.android.journal.view.AbstractApiActionBarActivity;
import com.despectra.android.journal.view.SimpleInfoDialog;
import com.despectra.android.journal.view.AbstractApiFragment;
import com.despectra.android.journal.view.users.StudentsFragment;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.customviews.BottomTabWidget;

import java.util.Arrays;

/**
 * Created by Dmitry on 12.04.14.
 */
public class GroupActivity extends AbstractApiActionBarActivity implements BottomTabWidget.OnTabSelectedListener {
    public static final String EXTRA_KEY_LOCAL_GROUP_ID = "locgroupId";
    public static final String EXTRA_KEY_REMOTE_GROUP_ID = "remgroupId";
    public static final String EXTRA_KEY_GROUP_NAME = "groupName";
    public static final String EXTRA_KEY_IS_SUBGROUP = "isSub";

    public static final String KEY_SELECTED_TAB = "selTab";

    public static final int TAB_GENERAL = 0;
    public static final int TAB_STUDENTS = 1;

    private BottomTabWidget mTabs;

    private long mLocalGroupId;
    private long mRemoteGroupId;
    private String mGroupName;
    private String mTitle;
    private boolean mIsSubgroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mLocalGroupId = getIntent().getLongExtra(EXTRA_KEY_LOCAL_GROUP_ID, -1);
        mRemoteGroupId = getIntent().getLongExtra(EXTRA_KEY_REMOTE_GROUP_ID, -1);
        mGroupName = getIntent().getStringExtra(EXTRA_KEY_GROUP_NAME);
        if (mRemoteGroupId == -1 || mLocalGroupId == -1) {
            SimpleInfoDialog errorDialog = SimpleInfoDialog.newInstance("Ошибка", "Нет идентификатора класса");
            errorDialog.show(getSupportFragmentManager(), "errorDialog");
            return;
        }
        mIsSubgroup = getIntent().getBooleanExtra(EXTRA_KEY_IS_SUBGROUP, false);
        mTitle = (mIsSubgroup ? "Группа " : "Класс ") + mGroupName;
        setTitle(mTitle);
        mTabs = (BottomTabWidget) findViewById(R.id.bottom_tabs);
        mTabs.setTabsList(Arrays.asList("Общее", "Ученики"));
        mTabs.setOnTabSelectedListener(this);
        if (savedInstanceState != null) {
            mTabs.setCurrentTab(savedInstanceState.getInt(KEY_SELECTED_TAB));
        } else {
            mTabs.setCurrentTab(0);
            GroupInfoFragment fragment = GroupInfoFragment.newInstance(mRemoteGroupId, mTitle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_layout, fragment, GroupInfoFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, mTabs.getCurrentTabIndex());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(int index) {
        AbstractApiFragment fragment;
        switch (index) {
            case TAB_GENERAL:
                fragment = GroupInfoFragment.newInstance(mRemoteGroupId, mTitle);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_layout, fragment, GroupInfoFragment.FRAGMENT_TAG)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
                break;
            case TAB_STUDENTS:
                fragment = StudentsFragment.newInstance(mLocalGroupId, mRemoteGroupId);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_layout, fragment, GroupInfoFragment.FRAGMENT_TAG)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
                break;
        }
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {

    }
}
