package com.despectra.android.journal.view.groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.AbstractApiFragment;

/**
 * Created by Dmitry on 13.04.14.
 */
public class GroupInfoFragment extends AbstractApiFragment {

    public static final String FRAGMENT_TAG = "groupInfoFrag";
    private static final String KEY_GROUP_ID = "grouID";
    public static final String KEY_GROUP_NAME = "groupName";

    private TextView mGroupNameView;

    private long mGroupId;
    private String mGroupName;

    public GroupInfoFragment() {
        super();
    }

    public static GroupInfoFragment newInstance(long groupRemoteId, String groupName) {
        GroupInfoFragment fragment = new GroupInfoFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_GROUP_ID, groupRemoteId);
        args.putString(KEY_GROUP_NAME, groupName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mGroupId = args.getLong(KEY_GROUP_ID);
        mGroupName = args.getString(KEY_GROUP_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);
        mGroupNameView = (TextView) view.findViewById(R.id.group_name);
        mGroupNameView.setText(mGroupName);

        return view;
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {

    }
}
