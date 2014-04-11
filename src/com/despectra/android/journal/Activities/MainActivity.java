package com.despectra.android.journal.Activities;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Dialogs.SimpleProgressDialog;
import com.despectra.android.journal.Fragments.GroupsFragment;
import com.despectra.android.journal.Fragments.MainPageFragment;
import com.despectra.android.journal.Fragments.StaffFragment;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Server.ServerAPI;
import com.despectra.android.journal.Services.ApiServiceHelper;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;

/**
 * Created by Dmitry on 25.03.14.
 */
public class MainActivity extends AbstractApiActivity implements AdapterView.OnItemClickListener {

    public static final String[] USER_DATA_PREFS_KEYS = new String[]{"token", "uid", "name", "surname", "middlename", "level", "avatar"};

    public static final int ACTION_EVENTS = 1;
    public static final int ACTION_STAFF = 2;
    public static final int ACTION_GROUPS = 3;
    public static final int ACTION_JOURNAL = 4;
    public static final int ACTION_SCHEDULE = 5;
    public static final int ACTION_SETTINGS = 6;
    public static final int ACTION_ABOUT = 7;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_LOGGING_OUT = 1;

    public static final String PROGRESS_DIALOG_TAG = "progressDialog";
    public static final String FRAGMENT_EVENTS = "EventsFragment";
    public static final String FRAGMENT_JOURNAL = "JournalFragment";
    private static final String FRAGMENT_SCHEDULE = "ScheduleFragment";
    private static final String FRAGMENT_GROUPS = "GroupsFragment";

    public static final String KEY_CUR_FRAGMENT = "curFragment";
    public static final String KEY_SELECTED_DRAWER_ITEM = "selectedDrawer";
    public static final String KEY_AB_TITLE = "actionBarTitle";
    public static final String KEY_STATUS = "status";
    private static final String TAG = "MainActivity";

    private Fragment mCurrentFragment;
    private String mActionBarTitle;
    private String mCurrentFragmentTag;
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
            String savedFragmentTag = savedInstanceState.getString(KEY_CUR_FRAGMENT);
            mCurrentFragment = restoreFragment(savedFragmentTag);
            mCurrentFragmentTag = savedFragmentTag;
            mActionBarTitle = savedInstanceState.getString(KEY_AB_TITLE);
            mStatus = savedInstanceState.getInt(KEY_STATUS);
            mLoadWall = false;
        } else {
            restoreDrawerState(ACTION_EVENTS);
            mCurrentFragment = new MainPageFragment();
            mCurrentFragmentTag = FRAGMENT_EVENTS;
            mActionBarTitle = "Главная";
            mStatus = STATUS_IDLE;
            mLoadWall = true;
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_layout, mCurrentFragment, mCurrentFragmentTag);
            ft.commit();
        }

        restoreActionBar();
        restoreSpinnerDialog();
    }

    /*private void testSchedFromJson() {
        try {
            Scanner scanner = new Scanner(getApplicationContext().getResources().openRawResource(R.raw.dayschedule));
            StringBuilder stringBuilder = new StringBuilder();
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine());
            }
            JSONObject json = new JSONObject(stringBuilder.toString());
            DaySchedule sched = DaySchedule.fromJson(json);
            Log.v(TAG, sched.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void testWeekSchedFromJson() {
        try {
            Scanner scanner = new Scanner(getApplicationContext().getResources().openRawResource(R.raw.weekschedule));
            StringBuilder stringBuilder = new StringBuilder();
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine());
            }
            JSONObject json = new JSONObject(stringBuilder.toString());
            WeekSchedule sched = WeekSchedule.fromJson(json);
            Log.v(TAG, sched.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }*/


    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_DRAWER_ITEM, mSelectedDrawerItem);
        outState.putString(KEY_CUR_FRAGMENT, mCurrentFragmentTag);
        outState.putString(KEY_AB_TITLE, mActionBarTitle);
        outState.putInt(KEY_STATUS, mStatus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void restoreSpinnerDialog() {
        mProgressDialog = (SimpleProgressDialog) getFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
        if (mProgressDialog == null) {
            mProgressDialog = SimpleProgressDialog.newInstance("Выход из системы");
            mProgressDialog.setCancelable(false);
        }
        updateProgressDialog();
    }

    private void updateProgressDialog() {
        switch (mStatus) {
            case STATUS_LOGGING_OUT:
                if (!mProgressDialog.isAdded()) {
                    mProgressDialog.show(getFragmentManager(), PROGRESS_DIALOG_TAG);
                }
                mProgressDialog.setMessage("Выход из системы..");
                break;
            case STATUS_IDLE:
                if (mProgressDialog.isAdded()) {
                    mProgressDialog.dismiss();
                }
        }
    }

    private void restoreActionBar() {
        getActionBar().setTitle(mActionBarTitle);
    }

    private void restoreDrawerState(int savedSelectedItem) {
        mDrawer.setSelection(savedSelectedItem);
        mSelectedDrawerItem = savedSelectedItem;
    }

    private Fragment restoreFragment(String savedFragmentTag) {
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentByTag(savedFragmentTag);
        if (f == null) {
            if (savedFragmentTag.equals(FRAGMENT_EVENTS)) {
                f = new MainPageFragment();
            } else if(savedFragmentTag.equals(FRAGMENT_GROUPS)) {
                f = new GroupsFragment();
            }
        }
        return f;
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
        switch(mSelectedDrawerItem) {
            case ACTION_STAFF:
                mCurrentFragment = new StaffFragment();
                mCurrentFragmentTag = StaffFragment.FRAGMENT_TAG;
                mActionBarTitle = "Персонал";
                break;
            case ACTION_EVENTS:
                mCurrentFragment = new MainPageFragment();
                mCurrentFragmentTag = FRAGMENT_EVENTS;
                mActionBarTitle = "Главная";
                break;
            case ACTION_GROUPS:
                mCurrentFragment = new GroupsFragment();
                mCurrentFragmentTag = GroupsFragment.FRAGMENT_TAG;
                mActionBarTitle = "Классы";
                break;
            /*case ACTION_JOURNAL:
                mCurrentFragment = new JournalFragment();
                mCurrentFragmentTag = FRAGMENT_JOURNAL;
                mActionBarTitle = "Журналы";
                break;
            case ACTION_SCHEDULE:
                mCurrentFragment = new ScheduleFragment();
                mCurrentFragmentTag = FRAGMENT_SCHEDULE;
                mActionBarTitle = "Расписание";*/
        }
        replaceCurrentFragment();
        restoreActionBar();
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
        //MEGA HACK, GOOGLE SUCK A DICK!!!
        try {
            Field mDragger = mDrawerLayout.getClass().getDeclaredField("mLeftDragger");
            mDragger.setAccessible(true);
            ViewDragHelper draggerObj = (ViewDragHelper) mDragger.get(mDrawerLayout);
            Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
            mEdgeSize.setAccessible(true);
            int edge = mEdgeSize.getInt(draggerObj);
            mEdgeSize.setInt(draggerObj, edge * 3);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        //
        mDrawer = (ListView)findViewById(R.id.nav_drawer);
        mDrawerUserItemLayout = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.user_drawer_item, null);
        mDrawer.addHeaderView(mDrawerUserItemLayout);
        mUserAvatarView = (ImageView) mDrawerUserItemLayout.findViewById(R.id.user_avatar);
        mUserSurnameView = (TextView) mDrawerUserItemLayout.findViewById(R.id.user_surname);
        mUserNameView = (TextView) mDrawerUserItemLayout.findViewById(R.id.user_name);
        mDrawer.setAdapter(new ArrayAdapter<String>(
                this,
                R.layout.drawer_item,
                new String[]{"Главная", "Персонал", "Классы", "Журнал", "Расписание", "Настройки", "О программе"}
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
            mUserAvatarView.setImageBitmap(BitmapFactory.decodeStream(openFileInput(ServerAPI.AVATAR_FILENAME)));
        } catch (FileNotFoundException ex) {
            mUserAvatarView.setImageDrawable(getResources().getDrawable(R.drawable.test_ava));
        }
        mUserSurnameView.setText(mSurname);
        mUserNameView.setText(mName);
    }

    private void replaceCurrentFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_layout, mCurrentFragment, mCurrentFragmentTag)
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

    private void respondError(String errorMsg) {
        setStatus(STATUS_IDLE);
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {
        if (actionCode != -1) {
            switch (actionCode) {
                case APICodes.ACTION_LOGOUT:
                    try {
                        JSONObject jsonData = (JSONObject) response;
                        int success = jsonData.getInt("success");
                        if (success == 1) {
                            completeLogout();
                        } else {
                            respondError("Ошибка при выходе. Попробуйте еще раз");
                        }
                    } catch (Exception ex) {
                        respondError("Ошибка " + ex.getMessage());
                    }
                    break;
            }
        }
    }
}
