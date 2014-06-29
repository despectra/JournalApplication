package com.despectra.android.journal.view.journal;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.*;
import android.support.v4.widget.CursorAdapter;
import android.view.*;
import android.widget.*;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.R;
import com.despectra.android.journal.model.*;
import com.despectra.android.journal.utils.MockCursorAdapter;
import com.despectra.android.journal.view.*;
import com.despectra.android.journal.utils.Utils;
import com.google.common.collect.Queues;

import java.util.*;

public class JournalFragment extends PagerContainerFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_TSG = 0;
    public static final String TAG = "JournalFragment";
    private EntityIds mSelectedSubject;
    private EntityIds mSelectedGroup;
    private EntityIds mSelectedTeacher;
    private EntityIds mSelectedTSG;
    private boolean mShowTeachers;
    private JournalAreaFragment mJournalAreaFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();

        mSelectedGroup = EntityIds.fromBundle(args.getBundle("group"));
        mShowTeachers = args.getBoolean("showTeachers");
        /*if (savedInstanceState != null) {
            mSelectedSubject = EntityIds.fromBundle(savedInstanceState.getBundle("selectedSubject"));
            mSelectedTeacher = EntityIds.fromBundle(savedInstanceState.getBundle("selectedTeacher"));
        }*/
        if (!mShowTeachers) {
            onTeacherSelected(EntityIds.fromBundle(args.getBundle("teacher")));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });
    }

    @Override
    public Fragment getPagerItem(int position) {
        Fragment fragment;
        Bundle args;
        switch (position) {
            case 0:
                fragment = new SubjectsFragment();
                args = new Bundle();
                args.putBundle("group", mSelectedGroup.toBundle());
                break;
            case 1:
                fragment = new JournalAreaFragment();
                args = new Bundle();
                args.putBundle("group", mSelectedGroup.toBundle());
                break;
            default:
                args = new Bundle();
                fragment = new Fragment();
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getPagerItemsCount() {
        return 2;
    }

    @Override
    public String getPagerItemTitle(int position) {
        switch (position) {
            case 0:
                return "Предмет";
            case 1:
                return "Журнал";
            default:
                return "";
        }
    }

    @Override
    public float getPagerItemWidth(int position) {
        switch (position) {
            case 0:
                return 0.25f;
            case 1:
                return 1;
            default:
                return 0;
        }
    }

    private void onSubjectClicked(EntityIds subjectIds) {
        if (subjectIds != null) {
            boolean showDialog = !subjectIds.equals(mSelectedSubject);
            mSelectedSubject = subjectIds;
            if (mShowTeachers && showDialog) {
                SpecifyTeacherDialog teacherDialog = SpecifyTeacherDialog.newInstance(getChildFragmentManager(), mSelectedSubject, mSelectedGroup);
                teacherDialog.show(getChildFragmentManager(), SpecifyTeacherDialog.TAG);
            }
        }
    }

    private void onTeacherSelected(EntityIds teacherIds) {
        if (teacherIds != null) {
            mSelectedTeacher = teacherIds;
            getLoaderManager().restartLoader(LOADER_TSG, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case LOADER_TSG:
                return new CursorLoader(getActivity(),
                        TSG.URI_ALL,
                        new String[]{TSG._ID, TSG.REMOTE_ID},
                        String.format("%s = ? AND %s = ? AND %s = ?",
                                TSG.FIELD_GROUP_ID, TeachersSubjects.FIELD_SUBJECT_ID, TeachersSubjects.FIELD_TEACHER_ID),
                        new String[]{
                                String.valueOf(mSelectedGroup.getLocalId()),
                                String.valueOf(mSelectedSubject.getLocalId()),
                                String.valueOf(mSelectedTeacher.getLocalId())},
                        null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor c) {
        if (c != null && c.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_TSG:
                    c.moveToFirst();
                    mSelectedTSG = new EntityIds(c.getLong(0), c.getLong(1));
                    updateJournalArea();
                    break;
            }
        }
    }

    private void updateJournalArea() {
        mJournalAreaFragment.updateArea(mSelectedTSG, mSelectedGroup);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    public void setJournalAreaFragment(JournalAreaFragment fragment) {
        mJournalAreaFragment = fragment;
    }

    public static class SpecifyTeacherDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor>,RemoteIdsCursorAdapter.OnItemClickListener {

        private static final String TAG = "SpecifyTeacherDialog";

        private JournalFragment mParentFragment;
        private EntityIds mSubjectIds;
        private EntityIds mGroupIds;
        private ListView mTeachersView;
        private Cursor mCursor;
        private SimpleRemoteIdsAdapter mTeachersAdapter;
        private View mRootView;

        public static SpecifyTeacherDialog newInstance(FragmentManager fm, EntityIds selectedSubject, EntityIds selectedGroup) {
            SpecifyTeacherDialog dialog = (SpecifyTeacherDialog) fm.findFragmentByTag(TAG);
            if (dialog == null) {
                dialog = new SpecifyTeacherDialog();
            }
            Bundle args = new Bundle();
            args.putBundle("subject", selectedSubject.toBundle());
            args.putBundle("group", selectedGroup.toBundle());
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            mSubjectIds = EntityIds.fromBundle(args.getBundle("subject"));
            mGroupIds = EntityIds.fromBundle(args.getBundle("group"));
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            mRootView = View.inflate(getActivity(), R.layout.fragment_simple_entities_list, null);
            mTeachersView = (ListView) mRootView.findViewById(R.id.entities_list_view);
            mTeachersView.setPadding(10, 10, 10, 10);
            builder.setTitle("Выберите учителя")
                    .setView(mRootView)
                    .setNegativeButton("Отмена", null);
            return builder.create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mTeachersAdapter = new SimpleRemoteIdsAdapter(getActivity(),
                    R.layout.item_simple_1,
                    mCursor,
                    new EntityIdsColumns[]{
                            new EntityIdsColumns(Teachers.TABLE, "_id", Teachers.REMOTE_ID)
                    },
                    Teachers.ENTITY_STATUS,
                    new String[]{"teacher_first_last_name"},
                    new int[]{R.id.text1},
                    0);
            mTeachersView.setAdapter(mTeachersAdapter);
            mParentFragment = (JournalFragment) getParentFragment();
            mTeachersAdapter.setOnItemClickListener(this);
            getLoaderManager().restartLoader(0, null, this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(getActivity(),
                    TSG.URI_ALL,
                    new String[]{
                            Teachers._ID + " as _id",
                            Teachers.REMOTE_ID,
                            String.format("%s||' '||%s||' '||%s as teacher_first_last_name",
                                    Users.FIELD_LAST_NAME, Users.FIELD_FIRST_NAME, Users.FIELD_MIDDLE_NAME),
                            Teachers.ENTITY_STATUS
                    },
                    String.format("%s = ? AND %s = ?", TSG.FIELD_GROUP_ID, TeachersSubjects.FIELD_SUBJECT_ID),
                    new String[]{String.valueOf(mGroupIds.getLocalId()), String.valueOf(mSubjectIds.getLocalId())},
                    "teacher_first_last_name ASC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mTeachersAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }

        @Override
        public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
            mParentFragment.onTeacherSelected(ids.getIdsByTable(Teachers.TABLE));
            dismiss();
        }
    }

    public static class SubjectsFragment extends AbstractApiFragment implements LoaderManager.LoaderCallbacks<Cursor>,
            RemoteIdsCursorAdapter.OnItemClickListener {

        private JournalFragment mParentFragment;
        private ListView mSubjectsView;
        private SimpleRemoteIdsAdapter mSubjectsAdapter;
        private EntityIds mGroupIds;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            mGroupIds = EntityIds.fromBundle(args.getBundle("group"));
            mParentFragment = (JournalFragment) getParentFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_simple_entities_list_frame, container, false);
            ((FrameLayout)v).setForeground(getResources().getDrawable(R.drawable.left_shadow));
            mSubjectsView = (ListView) v.findViewById(R.id.entities_list_view);
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mSubjectsAdapter = new SimpleRemoteIdsAdapter(getActivity(), R.layout.item_simple_1, mCursor,
                    new EntityIdsColumns[]{new EntityIdsColumns(Subjects.TABLE, "_id", Subjects.REMOTE_ID)},
                    Subjects.ENTITY_STATUS,
                    new String[]{Subjects.FIELD_NAME},
                    new int[]{R.id.text1},
                    RemoteIdsCursorAdapter.FLAG_SELECTABLE);
            mSubjectsView.setAdapter(mSubjectsAdapter);
            mSubjectsView.setPadding(Utils.dpToPx(getActivity(), 5), 0, 0, 0);
            mSubjectsAdapter.setOnItemClickListener(this);
            getLoaderManager().restartLoader(0, null, this);
        }

        @Override
        protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        }

        @Override
        protected void onResponseError(int actionCode, int remainingActions, Object response) {
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(getActivity(),
                    Subjects.URI_WITH_TSG,
                    new String[]{"DISTINCT " + Subjects._ID + " as _id", Subjects.REMOTE_ID,
                            Subjects.ENTITY_STATUS, Subjects.FIELD_NAME},
                    String.format("%s = ?", TSG.FIELD_GROUP_ID),
                    new String[]{String.valueOf(mGroupIds.getLocalId())},
                    Subjects.FIELD_NAME + " ASC"
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mSubjectsAdapter.swapCursor(cursor);
            if(cursor.getCount() > 0) {
                mSubjectsAdapter.setItemSelectedAtPos(0, cursor);
                mParentFragment.onSubjectClicked(mSubjectsAdapter.getItemIds(0).getIdsByTable(Subjects.TABLE));
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }

        @Override
        public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
            mParentFragment.onSubjectClicked(ids.getIdsByTable(Subjects.TABLE));
        }
    }

    /**
     * Created by Dmitry on 13.03.14.
     */
    public static class JournalAreaFragment extends AbstractApiFragment implements
            JournalPageFragment.JournalFragmentCallback,
            AbsListViewsDrawSynchronizer.Callback,
            LoaderManager.LoaderCallbacks<Cursor> {

        public static final String FRAGMENT_TAG = "JournalFragment";
        public static final int LOADER_STUDENTS = 0;
        public static final int LOADER_LESSONS = 1;
        public static final int LOADER_MARKS = 2;
        private static final int LOADER_DAYS = 3;



        private ListView mStudentsList;
        private ViewPager mPager;
        private JournalFragment mParentFragment;
        private JournalPagerAdapter mPagerAdapter;
        private AbsListViewsDrawSynchronizer mListsSync;
        private JournalPageFragment mMarksFragment;
        private int mFragmentGridPosition;
        private int mFragmentGridOffset;
        private boolean mIsPagerDragging;

        private EntityIds mCurrentTSG;
        private EntityIds mCurrentGroup;
        private SimpleRemoteIdsAdapter mStudentsAdapter;
        private Cursor mCursor;
        private Cursor mCursor2;
        private MockCursorAdapter mDaysHolderAdapter;
        private List<TimedWeekScheduleItem> mScheduleDaysModel;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mParentFragment = (JournalFragment) getParentFragment();
            mParentFragment.setJournalAreaFragment(this);
            Bundle args = getArguments();
            mCurrentGroup = EntityIds.fromBundle(args.getBundle("group"));
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_journal_full, container, false);

            mStudentsList = (ListView) rootView.findViewById(R.id.students_list_view);
            mPager = (ViewPager) rootView.findViewById(R.id.journal_pager);
            setHasOptionsMenu(true);

            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mStudentsAdapter = new SimpleRemoteIdsAdapter(getActivity(),
                    R.layout.item_simple_1,
                    mCursor,
                    new EntityIdsColumns[]{new EntityIdsColumns(Students.TABLE, "_id", Students.REMOTE_ID)},
                    Students.ENTITY_STATUS,
                    new String[]{"student_first_last_name"},
                    new int[]{R.id.text1},
                    0);
            mStudentsList.setAdapter(mStudentsAdapter);
            mDaysHolderAdapter = new MockCursorAdapter(getActivity(), mCursor2, 0);
            mIsPagerDragging = false;
            getLoaderManager().restartLoader(LOADER_STUDENTS, null, this);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (!mLoading) {
                mLoading = true;
                mServiceHelperController.getStudentsByGroup(mToken, mCurrentGroup, ApiServiceHelper.PRIORITY_HIGH);
                mServiceHelperController.getWeekScheduleForGroup(mToken, mCurrentGroup, ApiServiceHelper.PRIORITY_LOW);
            }
        }

        @Override
        public void onFragmentCreated(int index) {
            if(index == mPager.getCurrentItem()) {
                updateCurrentFragment();
            }
        }

        public void updateArea(EntityIds selectedTSG, EntityIds selectedGroup) {
            mCurrentTSG = selectedTSG;
            mCurrentGroup = selectedGroup;
            getLoaderManager().restartLoader(LOADER_DAYS, null, this);
        }

        private void updateCurrentFragment() {
            for (Fragment frag : getChildFragmentManager().getFragments()) {
                if (frag instanceof JournalPageFragment
                        && ((JournalPageFragment) frag).getIndex() == mPager.getCurrentItem()) {
                    mMarksFragment = ((JournalPageFragment) frag);
                    break;
                }
            }
            updateListsDrawSynchronizer();
        }

        private void updateListsDrawSynchronizer() {
            if (mListsSync != null) {
                mListsSync.setNewViews(mStudentsList, mMarksFragment.getMarksGridView());
            } else {
                mListsSync = new AbsListViewsDrawSynchronizer(
                        getActivity(),
                        mStudentsList,
                        mMarksFragment.getMarksGridView());
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
        protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {

        }

        @Override
        protected void onResponseError(int actionCode, int remainingActions, Object response) {

        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case LOADER_STUDENTS:
                    return new CursorLoader(getActivity(),
                            Uri.parse(String.format("%s/groups/%d/students", Contract.STRING_URI, mCurrentGroup.getLocalId())),
                            new String[]{Students._ID + " AS _id", Students.REMOTE_ID,
                                String.format("%s||' '||SUBSTR(%s, 1, 1)||'. '||SUBSTR(%s, 1, 1)||'.' as student_first_last_name",
                                        Users.FIELD_LAST_NAME, Users.FIELD_FIRST_NAME, Users.FIELD_MIDDLE_NAME),
                                Students.ENTITY_STATUS},
                            StudentsGroups.FIELD_GROUP_ID + " = ?",
                            new String[]{String.valueOf(mCurrentGroup.getLocalId())},
                            "student_first_last_name ASC" );
                case LOADER_DAYS:
                    return new CursorLoader(getActivity(),
                            Schedule.URI,
                            new String[]{Schedule._ID + " as _id", Schedule.REMOTE_ID, Schedule.FIELD_DAY, Schedule.FIELD_LESSON_NUMBER},
                            Schedule.FIELD_TSG_ID + " = ?",
                            new String[]{String.valueOf(mCurrentTSG.getLocalId())},
                            Schedule.FIELD_DAY + ", " + Schedule.FIELD_LESSON_NUMBER + " DESC" );
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            switch (cursorLoader.getId()) {
                case LOADER_STUDENTS:
                    mStudentsAdapter.swapCursor(cursor);
                    break;
                case LOADER_DAYS:
                    mDaysHolderAdapter.swapCursor(cursor);
                    mScheduleDaysModel = new LinkedList<TimedWeekScheduleItem>();
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        do {
                            TimedWeekScheduleItem item = new TimedWeekScheduleItem(cursor.getInt(2), cursor.getInt(3),
                                    new EntityIds(cursor.getLong(0), cursor.getLong(1)));
                            mScheduleDaysModel.add(item);
                        } while (cursor.moveToNext());

                        mPagerAdapter = new JournalPagerAdapter(this, getChildFragmentManager(), mCurrentTSG, mScheduleDaysModel);
                        mPager.setAdapter(mPagerAdapter);
                        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                            @Override
                            public void onPageScrollStateChanged(int state) {
                                super.onPageScrollStateChanged(state);
                                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                                    mIsPagerDragging = true;
                                    List<Fragment> frags = getChildFragmentManager().getFragments();
                                    for (Fragment frag : frags) {
                                        if (frag instanceof JournalPageFragment) {
                                            JournalPageFragment fragment = (JournalPageFragment) frag;
                                            if (fragment.getIndex() != mPager.getCurrentItem()) {
                                                fragment.setMarksGridScrolling(mFragmentGridPosition, mFragmentGridOffset);
                                            }
                                        }
                                    }
                                } else if (state == ViewPager.SCROLL_STATE_IDLE) {
                                    mIsPagerDragging = false;
                                    updateCurrentFragment();
                                }
                            }
                        });
                    }
                    break;

            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }


    }
}
