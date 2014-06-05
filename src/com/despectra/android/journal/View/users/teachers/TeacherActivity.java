package com.despectra.android.journal.view.users.teachers;

import android.content.Intent;
import android.os.Bundle;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.AbstractApiActionBarActivity;

/**
 * Created by Dmitry on 01.06.14.
 */
public class TeacherActivity extends AbstractApiActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abstract);
        if (savedInstanceState == null) {
            TeacherFragment fragment = new TeacherFragment();
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            Bundle userIds = extras.getBundle("userId");

            Bundle args = new Bundle();
            args.putBundle("userId", userIds);
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_container, fragment, "AbstractUserFragment")
                    .commit();
        }
    }

    @Override
    protected boolean showUpButton() {
        return true;
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {

    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {

    }
}
