package com.despectra.android.journal.view.users.teachers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        //TODO transition animation
        listView.setEnabled(false);
        TextView itemTextView = (TextView) clickedSubjectView.findViewById(R.id.text1);
        mLinkingHeader.setVisibility(View.VISIBLE);
        mLinkingHeader.setY(clickedSubjectView.getY());
        mLinkingTextView.setText(itemTextView.getText());
        mLinkingHeader.animate().alpha(1).setDuration(300).start();
        mLinkingHeader.animate().y(0).setDuration(300).start();
        clickedSubjectView.animate().alpha(0).setDuration(300).withEndAction(new Runnable() {
            @Override
            public void run() {
                moveFragmentLayout(true);
                clickedSubjectView.setVisibility(View.GONE);

                Bundle args = new Bundle();
                args.putBundle("teacher_subject_id", ids.getIdsByTable("teachers_subjects").toBundle());
                GroupsForSubjectFragment fragment = new GroupsForSubjectFragment();
                fragment.setArguments(args);
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.fragment_layout, fragment, GroupsForSubjectFragment.TAG)
                        .addToBackStack(GroupsForSubjectFragment.TAG)
                        .commit();
            }
        }).start();
        boolean wasClicked = false;
        for (int i = 0; i < listView.getChildCount(); i++) {
            View view = listView.getChildAt(i);
            if (wasClicked) {
                view.animate().yBy(getView().getMeasuredHeight() - clickedSubjectView.getTop() + clickedSubjectView.getMeasuredHeight())
                        .setDuration(300).start();
            } else {
                view.animate().yBy(-clickedSubjectView.getTop()).setDuration(300).start();
            }
            wasClicked = wasClicked || (view == clickedSubjectView);
        }
    }

    private void closeGroupsList() {
        moveFragmentLayout(false);
        mLinkingHeader.setAlpha(0);
        mLinkingHeader.setVisibility(View.GONE);
        getChildFragmentManager().popBackStack();
    }

    private void moveFragmentLayout(boolean toBottom) {
        int _60dp = Utils.dpToPx(getActivity(), 60);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mFragmentLayout.getLayoutParams();
        params.topMargin = toBottom ? params.topMargin + _60dp : params.topMargin - _60dp;
        mFragmentLayout.setLayoutParams(params);
    }
}
