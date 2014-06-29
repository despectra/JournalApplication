package com.despectra.android.journal.view.journal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import com.despectra.android.journal.R;
import com.despectra.android.journal.model.EntityIds;

/**
 * Created by Dmitry on 28.06.14.
 */
public class GroupsJournalActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abstract);
        Intent intent = getIntent();
        EntityIds groupIds = EntityIds.fromBundle(intent.getBundleExtra("groupIds"));
        boolean showForTeacher = intent.getBooleanExtra("forTeacher", false);
        EntityIds teacherIds = showForTeacher
                ? EntityIds.fromBundle(intent.getBundleExtra("teacherIds"))
                : null;
        String groupName = intent.getStringExtra("groupName");
        getSupportActionBar().setTitle("Журнал " + groupName);
        JournalFragment fragment = (JournalFragment) getSupportFragmentManager().findFragmentByTag(JournalFragment.TAG);
        if (fragment == null) {
            fragment = new JournalFragment();
        }
        Bundle args = new Bundle();
        args.putBoolean("showTeachers", !showForTeacher);
        args.putBundle("group", groupIds.toBundle());
        args.putBundle("teacher", showForTeacher ? teacherIds.toBundle() : null);
        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.activity_container, fragment, JournalFragment.TAG)
                    .commit();
            fragment.setArguments(args);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return true;
        }
    }
}
