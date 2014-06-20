package com.despectra.android.journal.view;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.*;
import android.widget.*;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.net.WebApiServer;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.groups.GroupsFragment;
import com.despectra.android.journal.view.main_page.MainPageFragmentFactory;
import com.despectra.android.journal.view.preferences.PreferencesActivity;
import com.despectra.android.journal.view.schedule.WeekScheduleFragment;
import com.despectra.android.journal.view.subjects.SubjectsFragment;
import com.despectra.android.journal.view.users.StaffFragment;
import org.json.JSONObject;

import java.io.FileNotFoundException;

/**
 * Created by Dmitry on 25.03.14.
 */
public class MainActivity extends AbstractApiActionBarActivity implements AdapterView.OnItemClickListener {

    public static final int ACTION_EVENTS = 1;
    public static final int ACTION_STAFF = 2;
    public static final int ACTION_GROUPS = 3;
    public static final int ACTION_SUBJECTS = 4;
    public static final int ACTION_JOURNAL = 5;
    public static final int ACTION_SCHEDULE = 6;
    public static final int ACTION_SETTINGS = 7;
    public static final int ACTION_ABOUT = 8;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_LOGGING_OUT = 1;

    public static final String PROGRESS_DIALOG_TAG = "progressDialog";

    public static final String KEY_SELECTED_DRAWER_ITEM = "selectedDrawer";
    public static final String KEY_STATUS = "status";
    private static final String TAG = "MainActivity";

    private String mToken;
    private int mUserId;
    private String mName;
    private String mSurname;
    private String mMiddlename;
    private int mLevel;
    private String mAvatar;

    private SimpleProgressDialog mProgressDialog;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawer;
    private RelativeLayout mDrawerUserItemLayout;
    private ImageView mUserAvatarView;
    private TextView mUserSurnameView;
    private TextView mUserNameView;
    private int mSelectedDrawerItem;
    private int mStatus;
    private boolean mLoadWall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readNewPreferences();
        initDrawer();

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState != null) {
            restoreDrawerState(savedInstanceState.getInt(KEY_SELECTED_DRAWER_ITEM));
            mStatus = savedInstanceState.getInt(KEY_STATUS);
            mLoadWall = false;
        } else {
            restoreDrawerState(ACTION_EVENTS);
            mStatus = STATUS_IDLE;
            mLoadWall = true;
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_layout,
                    MainPageFragmentFactory.instantiate(this, getSupportFragmentManager()),
                    MainPageFragmentFactory.FRAGMENT_TAG);
            ft.commit();
        }
        restoreSpinnerDialog();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_DRAWER_ITEM, mSelectedDrawerItem);
        outState.putInt(KEY_STATUS, mStatus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected boolean showUpButton() {
        return false;
    }

    private void restoreSpinnerDialog() {
        mProgressDialog = (SimpleProgressDialog) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
        if (mProgressDialog == null) {
            mProgressDialog = SimpleProgressDialog.newInstance("Выход из системы..");
            mProgressDialog.setCancelable(false);
        }
        updateProgressDialog();
    }

    private void updateProgressDialog() {
        switch (mStatus) {
            case STATUS_LOGGING_OUT:
                if (!mProgressDialog.isAdded()) {
                    mProgressDialog.show(getSupportFragmentManager(), PROGRESS_DIALOG_TAG);
                }
                mProgressDialog.setMessage("Выход из системы..");
                break;
            case STATUS_IDLE:
                if (mProgressDialog.isAdded()) {
                    mProgressDialog.dismiss();
                }
        }
    }

    private void restoreDrawerState(int savedSelectedItem) {
        mDrawer.setSelection(savedSelectedItem);
        mSelectedDrawerItem = savedSelectedItem;
    }

    private void setStatus(int status) {
        mStatus = status;
        updateProgressDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_logout:
                performLogout();
                break;
            case R.id.TEMP_clear_local_DB:
                String login = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_LOGIN, "");
                final SQLiteDatabase db = this.openOrCreateDatabase(login, MODE_PRIVATE, null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.clearLocalDB(db);
                    }
                }).run();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performLogout() {
        setStatus(STATUS_LOGGING_OUT);
        mServiceHelperController.logout(mToken, ApiServiceHelper.PRIORITY_LOW);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        mDrawerLayout.closeDrawers();
        if(position == mSelectedDrawerItem) {
            return;
        }
        if (position == ACTION_SETTINGS) {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
            return;
        }
        mDrawer.setSelection(mSelectedDrawerItem);
        mSelectedDrawerItem = position;
        Fragment fragment;
        String fragmentTag;
        switch(mSelectedDrawerItem) {
            case ACTION_STAFF:
                fragmentTag = StaffFragment.FRAGMENT_TAG;
                fragment = getSupportFragmentManager().findFragmentByTag(StaffFragment.FRAGMENT_TAG);
                if (fragment == null) {
                    fragment = new StaffFragment();
                }
                break;
            case ACTION_EVENTS:
                fragmentTag = MainPageFragmentFactory.FRAGMENT_TAG;
                fragment = MainPageFragmentFactory.instantiate(this, getSupportFragmentManager());
                break;
            case ACTION_GROUPS:
                fragmentTag = GroupsFragment.FRAGMENT_TAG;
                fragment = getSupportFragmentManager().findFragmentByTag(GroupsFragment.FRAGMENT_TAG);
                if (fragment == null) {
                    fragment = new GroupsFragment();
                }
                break;
            case ACTION_SUBJECTS:
                fragmentTag = SubjectsFragment.FRAGMENT_TAG;
                fragment = getSupportFragmentManager().findFragmentByTag(SubjectsFragment.FRAGMENT_TAG);
                if (fragment == null) {
                    fragment = new SubjectsFragment();
                }
                break;
            case ACTION_SCHEDULE:
                fragmentTag = WeekScheduleFragment.TAG;
                fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
                if (fragment == null) {
                    fragment = new WeekScheduleFragment();
                }
                break;
            default:
                return;
        }
        replaceCurrentFragment(fragment, fragmentTag);
    }

    private void readNewPreferences() {
        mToken = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        mName = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_NAME, "");
        mUserId = PreferenceManager.getDefaultSharedPreferences(this).getInt(JournalApplication.PREFERENCE_KEY_UID, 0);
        mSurname = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_SURNAME, "");
        mMiddlename = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_MIDDLENAME, "");
        mLevel = PreferenceManager.getDefaultSharedPreferences(this).getInt(JournalApplication.PREFERENCE_KEY_LEVEL, 0);
    }

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawer = (ListView)findViewById(R.id.nav_drawer);
        mDrawerUserItemLayout = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.user_drawer_item, null);
        mDrawer.addHeaderView(mDrawerUserItemLayout);
        mUserAvatarView = (ImageView) mDrawerUserItemLayout.findViewById(R.id.user_avatar);
        mUserSurnameView = (TextView) mDrawerUserItemLayout.findViewById(R.id.user_surname);
        mUserNameView = (TextView) mDrawerUserItemLayout.findViewById(R.id.user_name);
        mDrawer.setAdapter(new ArrayAdapter<String>(
                this,
                R.layout.drawer_item,
                new String[]{"Главная", "Персонал", "Классы", "Предметы", "Журнал", "Расписание", "Настройки", "О программе"}
        ));
        mDrawer.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mDrawer.setOnItemClickListener(this);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                0,
                0);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        loadUserData();
    }

    private void loadUserData() {
        try {
            mUserAvatarView.setImageBitmap(BitmapFactory.decodeStream(openFileInput(WebApiServer.AVATAR_FILENAME)));
        } catch (FileNotFoundException ex) {
            mUserAvatarView.setImageDrawable(getResources().getDrawable(R.drawable.empty_ava));
        }
        mUserSurnameView.setText(mSurname);
        mUserNameView.setText(mName);
    }

    private void replaceCurrentFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_layout, fragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
    }

    private void completeLogout() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .remove(JournalApplication.PREFERENCE_KEY_TOKEN)
                .remove(JournalApplication.PREFERENCE_KEY_LOGIN)
                .remove(JournalApplication.PREFERENCE_KEY_NAME)
                .remove(JournalApplication.PREFERENCE_KEY_MIDDLENAME)
                .remove(JournalApplication.PREFERENCE_KEY_SURNAME)
                .remove(JournalApplication.PREFERENCE_KEY_UID)
                .remove(JournalApplication.PREFERENCE_KEY_LEVEL)
                .commit();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {
        completeLogout();
    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        ApiErrorResponder.respondDialog(getSupportFragmentManager(), (JSONObject)response);
    }
}
