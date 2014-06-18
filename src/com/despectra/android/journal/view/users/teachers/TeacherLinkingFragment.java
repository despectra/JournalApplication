package com.despectra.android.journal.view.users.teachers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;
import com.despectra.android.journal.R;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.LinksFragment;
import com.despectra.android.journal.view.groups.GroupsForSubjectFragment;
import com.despectra.android.journal.view.subjects.SubjectsOfTeacherFragment;

/**
 * Created by Андрей on 16.06.14.
 */
public class TeacherLinkingFragment extends Fragment {

    private final LinksFragment.OnItemClickedListener mSubjFragmentItemClickListener = new LinksFragment.OnItemClickedListener() {
        @Override
        public void onItemClicked(ListView listView, int position, View clickedItemView, JoinedEntityIds ids) {
            openGroupsList(listView, position, clickedItemView, ids);
        }
    };
    private RelativeLayout mLinkingHeader;
    private TextView mLinkingTextView;
    private ImageButton mLinkingDoneBtn;
    private FrameLayout mFragmentLayout;
    private ListView mSubjectsView;
    private View mLastClickedSubjView;

    private boolean mOpening;
    private boolean mClosing;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_linking, container, false);
        mFragmentLayout = (FrameLayout) view.findViewById(R.id.fragment_layout);
        mLinkingHeader = (RelativeLayout) view.findViewById(R.id.linking_header);
        mLinkingTextView = (TextView) view.findViewById(R.id.linking_header_text);
        mLinkingDoneBtn = (ImageButton) view.findViewById(R.id.linking_header_done_btn);
        mLinkingDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeGroupsList();
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mOpening = false;
        mClosing = false;
        if (savedInstanceState == null) {
            SubjectsOfTeacherFragment fragment = new SubjectsOfTeacherFragment();
            fragment.setArguments(getArguments());
            fragment.setOnItemClickedListener(mSubjFragmentItemClickListener);
            getChildFragmentManager().beginTransaction()
                    .add(R.id.fragment_layout, fragment, SubjectsOfTeacherFragment.TAG)
                    .commit();
        } else {
            Fragment fragment = getChildFragmentManager().findFragmentByTag(SubjectsOfTeacherFragment.TAG);
            if (fragment == null) {
                fragment = getChildFragmentManager().findFragmentByTag(GroupsForSubjectFragment.TAG);
            } else {
                ((SubjectsOfTeacherFragment)fragment).setOnItemClickedListener(mSubjFragmentItemClickListener);
            }

        }
    }

    private void openGroupsList(ListView listView, int position, final View clickedSubjectView, final JoinedEntityIds ids) {
        if (mOpening) {
            return;
        }
        mOpening = true;
        getChildFragmentManager().findFragmentByTag(SubjectsOfTeacherFragment.TAG).setHasOptionsMenu(false);

        mSubjectsView = listView;
        mLastClickedSubjView = clickedSubjectView;
        mSubjectsView.setEnabled(false);
        TextView itemTextView = (TextView) clickedSubjectView.findViewById(R.id.text1);
        mLinkingHeader.setVisibility(View.VISIBLE);
        mLinkingHeader.setY(clickedSubjectView.getY());
        mLinkingTextView.setText(itemTextView.getText());
        mLinkingHeader.animate().alpha(1).setDuration(300).start();
        mLinkingHeader.animate().y(0).setDuration(300).start();
        mLastClickedSubjView.animate().alpha(0).setDuration(300).withEndAction(new Runnable() {
            @Override
            public void run() {
                moveFragmentLayout(true);
                clickedSubjectView.setVisibility(View.GONE);

                Bundle args = new Bundle();
                args.putBundle("teacher_subject_id", ids.getIdsByTable("teachers_subjects").toBundle());
                GroupsForSubjectFragment fragment = new GroupsForSubjectFragment();
                fragment.setArguments(args);
                getChildFragmentManager().beginTransaction()
                        .add(R.id.fragment_layout, fragment, GroupsForSubjectFragment.TAG)
                        .addToBackStack(GroupsForSubjectFragment.TAG)
                        .commit();
                mOpening = false;
            }
        }).start();
        boolean wasClicked = false;
        int moveUpTo = getView().getMeasuredHeight() - mLastClickedSubjView.getTop() + mLastClickedSubjView.getMeasuredHeight();
        int moveDownTo = -mLastClickedSubjView.getTop();
        for (int i = 0; i < mSubjectsView.getChildCount(); i++) {
            final View view = mSubjectsView.getChildAt(i);
            Runnable switchOffTrState = new Runnable() {
                @Override
                public void run() {
                    view.setHasTransientState(false);
                }
            };
            view.setHasTransientState(true);
            if (wasClicked) {
                view.setTag(moveUpTo);
                view.animate().yBy(moveUpTo).setDuration(300).withEndAction(switchOffTrState).start();
            } else {
                view.setTag(moveDownTo);
                view.animate().yBy(moveDownTo).setDuration(300).withEndAction(switchOffTrState).start();
            }
            wasClicked = wasClicked || (view == mLastClickedSubjView);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void closeGroupsList() {
        if (mClosing) {
            return;
        }
        mClosing = true;
        mFragmentLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mFragmentLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                mLastClickedSubjView.setVisibility(View.VISIBLE);
                mLastClickedSubjView.animate().alpha(1).setDuration(300).start();
                mLinkingHeader.animate().alpha(0).yBy(-(Integer) mLastClickedSubjView.getTag()).setDuration(300).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mLinkingHeader.setVisibility(View.GONE);
                        mSubjectsView.setEnabled(true);
                        getChildFragmentManager().findFragmentByTag(SubjectsOfTeacherFragment.TAG).setHasOptionsMenu(true);
                        mClosing = false;
                    }
                }).start();
                for (int i = 0; i < mSubjectsView.getChildCount(); i++) {
                    final View view = mSubjectsView.getChildAt(i);
                    Runnable switchOffTrState = new Runnable() {
                        @Override
                        public void run() {
                            view.setHasTransientState(false);
                        }
                    };
                    view.setHasTransientState(true);
                    view.animate().yBy(-(Integer) view.getTag()).setDuration(300).withEndAction(switchOffTrState).start();
                }
                getChildFragmentManager().popBackStack();
                return true;
            }
        });
        moveFragmentLayout(false);
    }

    private void moveFragmentLayout(boolean toBottom) {
        int _60dp = Utils.dpToPx(getActivity(), 60);
        /*RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mFragmentLayout.getLayoutParams();
        params.topMargin = toBottom ? params.topMargin + _60dp : params.topMargin - _60dp;
        mFragmentLayout.setLayoutParams(params);*/
        mFragmentLayout.setPadding(0, toBottom ? _60dp : 0, 0, 0);
    }
}
