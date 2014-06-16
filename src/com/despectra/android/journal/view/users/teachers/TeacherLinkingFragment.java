package com.despectra.android.journal.view.users.teachers;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.despectra.android.journal.R;

/**
 * Created by Андрей on 16.06.14.
 */
public class TeacherLinkingFragment extends Fragment {

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
}
