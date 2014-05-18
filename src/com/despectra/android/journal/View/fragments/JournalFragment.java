package com.despectra.android.journal.view.fragments;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import com.despectra.android.journal.Adapters.JournalPagerAdapter;
import com.despectra.android.journal.Data.Contract;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.AbsListViewsDrawSynchronizer;
import com.despectra.android.journal.Utils;
import com.despectra.android.journal.Views.BottomTabWidget;
import com.despectra.android.journal.Views.PercentLinearLayout;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Dmitry on 13.03.14.
 */
public class JournalFragment extends AbstractApiFragment implements
        JournalMarksFragment.JournalFragmentCallback,
        AbsListViewsDrawSynchronizer.Callback,
        BottomTabWidget.OnTabSelectedListener,
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "JOURNAL_FRAGMENT";

    public static final int GROUP = 0;
    public static final int MARKS = 1;
    public static final String FRAGMENT_TAG = "JournalFragment";

    private PercentLinearLayout mContentLayout;
    private ListView mGroupsList;
    private ListView mStudentsList;
    private ViewPager mPager;
    private BottomTabWidget mTabWidget;
    private JournalPagerAdapter mPagerAdapter;
    private AbsListViewsDrawSynchronizer mListsSync;
    private JournalMarksFragment mCurrentGridFragment;
    private int mFragmentGridPosition;
    private int mFragmentGridOffset;
    private boolean mIsPagerDragging;
    private int mUiState;

    private long mGroupId;
    private SimpleCursorAdapter mGroupsAdapter;
    private SimpleCursorAdapter mStudentsAdapter;
    private Cursor mCursor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_journal_full, container, false);

        mContentLayout = (PercentLinearLayout) rootView.findViewById(R.id.journal_content_layout);
        mGroupsList = (ListView) rootView.findViewById(R.id.groups_list_view);
        mStudentsList = (ListView) rootView.findViewById(R.id.students_list_view);
        mPager = (ViewPager) rootView.findViewById(R.id.journal_pager);
        mPagerAdapter = new JournalPagerAdapter(getFragmentManager());
        mTabWidget = (BottomTabWidget) rootView.findViewById(R.id.journal_tabs);
        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGroupsAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_1,
                mCursor,
                new String[]{Contract.Groups.FIELD_NAME},
                new int[]{android.R.id.text1},
                0);
        mGroupsList.setAdapter(mGroupsAdapter);
        mGroupsList.setOnItemClickListener(this);

        mStudentsAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.journal_student_item,
                mCursor,
                new String[]{"nameSurname"},
                new int[]{R.id.text1},
                0);
        mStudentsList.setAdapter(mStudentsAdapter);

        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(mPagerAdapter.getCount() - 1);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if(state == ViewPager.SCROLL_STATE_DRAGGING) {
                    mIsPagerDragging = true;
                    List<Fragment> frags = getFragmentManager().getFragments();
                    for (Fragment frag : frags) {
                        if (frag instanceof JournalMarksFragment) {
                            JournalMarksFragment fragment = (JournalMarksFragment) frag;
                            if(fragment.getIndex() != mPager.getCurrentItem()) {
                                fragment.setMarksGridScrolling(mFragmentGridPosition, mFragmentGridOffset);
                            }
                        }
                    }
                } else if(state == ViewPager.SCROLL_STATE_IDLE) {
                    mIsPagerDragging = false;
                    updateCurrentFragment();
                }
            }
        });

        mIsPagerDragging = false;
        mTabWidget.setTabsList(Arrays.asList(new String[]{"Класс", "Оценки"}));
        mTabWidget.setCurrentTab(1);
        mTabWidget.setOnTabSelectedListener(this);
        mUiState = MARKS;

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mContentLayout.setTranslationXByPercent(-30);
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onFragmentCreated(int index) {
        if(index == mPager.getCurrentItem()) {
            updateCurrentFragment();
        }
    }

    public long getGroupId() {
        return mGroupId;
    }

    private void updateCurrentFragment() {
        for (Fragment frag : getFragmentManager().getFragments()) {
            if (frag instanceof JournalMarksFragment
                    && ((JournalMarksFragment) frag).getIndex() == mPager.getCurrentItem()) {
                mCurrentGridFragment = ((JournalMarksFragment) frag);
                break;
            }
        }
        updateListsDrawSynchronizer();
    }

    private void updateListsDrawSynchronizer() {
        if (mListsSync != null) {
            mListsSync.setNewViews(mStudentsList, mCurrentGridFragment.getMarksGridView());
        } else {
            mListsSync = new AbsListViewsDrawSynchronizer(
                    getActivity(),
                    mStudentsList,
                    mCurrentGridFragment.getMarksGridView());
            mListsSync.setCallback(this);
        }
    }

    @Override
    public void onScrollingStopped(AbsListView view) {
        if (!mIsPagerDragging) {
            mFragmentGridPosition = view.getFirstVisiblePosition();
            mFragmentGridOffset = Utils.getAbsListViewOffset(view);
        }
    }

    @Override
    public void onTabSelected(final int index) {
        if (index != mUiState) {
            mContentLayout.smoothScrollByPercent(
                    (index == GROUP) ? 30 : -30,
                    250,
                    null,
                    new Runnable() {
                        @Override
                        public void run() {
                            mUiState = index;
                        }
                    });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_students_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_student:
                mServiceHelperController.addMockMarks(mGroupId);
                break;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView == mGroupsList) {
            mContentLayout.smoothScrollByPercent(
                    -30,
                    250,
                    null,
                    new Runnable() {
                        @Override
                        public void run() {
                            mUiState = MARKS;
                        }
                    });
            mTabWidget.setCurrentTab(1);
            mGroupId = l;
            getLoaderManager().restartLoader(1, null, this);
        }
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case 0:
                return new CursorLoader(getActivity(),
                        Contract.Groups.URI,
                        new String[]{Contract.Groups._ID + " AS _id", Contract.Groups.FIELD_NAME},
                        null,
                        null,
                        null
                );
            case 1:
                mCurrentGridFragment.setGroupId(mGroupId);
                mCurrentGridFragment.updateMarks();
                return new CursorLoader(getActivity(),
                        Uri.parse(String.format("%s/groups_remote/%d/students_remote", Contract.STRING_URI, mGroupId)),
                        new String[]{Contract.Students._ID + " AS _id", Contract.Students.Remote.REMOTE_ID,
                                Contract.Users.FIELD_SURNAME + "||\" \"||SUBSTR(" + Contract.Users.FIELD_NAME + ", 1, 1)||\".\" AS nameSurname",
                                Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_LOGIN,
                                Contract.Users.ENTITY_STATUS},
                        Contract.StudentsGroups.FIELD_GROUP_ID + " = ?",
                        new String[]{String.valueOf(mGroupId)},
                        Contract.Users.FIELD_SURNAME + " ASC");
            case 2:

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch (cursorLoader.getId()) {
            case 0:
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    mGroupsAdapter.swapCursor(cursor);
                    mGroupId = cursor.getLong(0);
                    getLoaderManager().initLoader(1, null, this);
                }
                break;
            case 1:
                mStudentsAdapter.swapCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }
}
