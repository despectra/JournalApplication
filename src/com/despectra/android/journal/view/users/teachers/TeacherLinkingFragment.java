package com.despectra.android.journal.view.users.teachers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.view.LinksFragment;
import com.despectra.android.journal.view.groups.GroupsForSubjectFragment;
import com.despectra.android.journal.view.subjects.SubjectsOfTeacherFragment;

/**
 * Created by Андрей on 16.06.14.
 */
public class TeacherLinkingFragment extends Fragment {

    private final LinksFragment.OnItemClickedListener mSubjFragmentItemClickListener = new LinksFragment.OnItemClickedListener() {
        @Override
        public void onItemClicked(View clickedItemView, JoinedEntityIds ids) {
            openGroupsList(clickedItemView, ids);
        }
    };
    private RelativeLayout mLinkingHeader;
    private TextView mLinkingTextView;
    private ImageButton mLinkingDoneBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_linking, container, false);
        mLinkingHeader = (RelativeLayout) view.findViewById(R.id.linking_header);
        mLinkingTextView = (TextView) view.findViewById(R.id.linking_header_text);
        mLinkingDoneBtn = (ImageButton) view.findViewById(R.id.linking_header_done_btn);
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

    private void openGroupsList(View clickedSubjectView, JoinedEntityIds ids) {
        //TODO transition animation
        Bundle args = new Bundle();
        args.putBundle("teacher_subject_id", ids.getIdsByTable("teachers_subjects").toBundle());
        GroupsForSubjectFragment fragment = new GroupsForSubjectFragment();
        fragment.setArguments(args);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_layout, fragment, GroupsForSubjectFragment.TAG)
                .commit();
    }
}
