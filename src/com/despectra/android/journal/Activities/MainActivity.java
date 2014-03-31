package com.despectra.android.journal.Activities;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.*;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Dialogs.SimpleProgressDialog;
import com.despectra.android.journal.Fragments.MainPageFragment;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Server.ServerAPI;
import com.despectra.android.journal.Services.ApiServiceHelper;
import org.json.JSONObject;

import java.io.FileNotFoundException;

/**
 * Created by Dmitry on 25.03.14.
 */
public class MainActivity extends ApiActivity implements AdapterView.OnItemClickListener, ApiServiceHelper.Callback {

    public static final String[] USER_DATA_PREFS_KEYS = new String[]{"token", "uid", "name", "surname", "middlename", "level", "avatar"};

    public static final int ACTION_EVENTS = 1;
    public static final int ACTION_JOURNAL = 2;
    public static final int ACTION_SCHEDULE = 3;
    public static final int ACTION_GROUPS = 4;
    public static final int ACTION_SETTINGS = 5;
    public static final int ACTION_ABOUT = 6;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_LOGGING_OUT = 1;

    public static final String PROGRESS_DIALOG_TAG = "progressDialog";
    public static final String FRAGMENT_EVENTS = "EventsFragment";
    public static final String FRAGMENT_JOURNAL = "JournalFragment";
    private static final String FRAGMENT_SCHEDULE = "ScheduleFragment";

    public static final String KEY_CUR_FRAGMENT = "curFragment";
    public static final String KEY_SELECTED_DRAWER_ITEM = "selectedDrawer";
    public static final String KEY_AB_TITLE = "actionBarTitle";
    public static final String KEY_STATUS = "status";
    private static final String TAG = "MAIN_ACTIVITY";

    /*private MainPageFragment mEventsFragment;
    private JournalFragment mJournalFragment;*/

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
        } else {
            restoreDrawerState(ACTION_EVENTS);
            mCurrentFragment = new MainPageFragment();
            mCurrentFragmentTag = FRAGMENT_EVENTS;
            mActionBarTitle = "Главная";
            mStatus = STATUS_IDLE;
        }

        restoreActionBar();
        restoreSpinnerDialog();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_layout, mCurrentFragment, mCurrentFragmentTag);
        ft.commit();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mServiceHelperController = mApplicationContext.getApiServiceHelper().registerActivity(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterActivity(this);
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
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentByTag(savedFragmentTag);
        if (f == null) {
            if (savedFragmentTag.equals(FRAGMENT_EVENTS)) {
                f = new MainPageFragment();
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
        mServiceHelperController.logout(mToken);
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


        /*switch(mSelectedDrawerItem) {
            case ACTION_EVENTS:
                mCurrentFragment = new MainPageFragment();
                mCurrentFragmentTag = FRAGMENT_EVENTS;
                mActionBarTitle = "Стена";
                break;
            case ACTION_JOURNAL:
                mCurrentFragment = new JournalFragment();
                mCurrentFragmentTag = FRAGMENT_JOURNAL;
                mActionBarTitle = "Журналы";
                break;
            case ACTION_SCHEDULE:
                mCurrentFragment = new ScheduleFragment();
                mCurrentFragmentTag = FRAGMENT_SCHEDULE;
                mActionBarTitle = "Расписание";
        }
        replaceCurrentFragment();
        restoreActionBar();*/
    }

    private void readNewPreferences() {
        mToken = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        mName = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_NAME, "");
        mUserId = PreferenceManager.getDefaultSharedPreferences(this).getInt(JournalApplication.PREFERENCE_KEY_UID, 0);
        mSurname = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_SURNAME, "");
        mMiddlename = PreferenceManager.getDefaultSharedPreferences(this).getString(JournalApplication.PREFERENCE_KEY_MIDDLENAME, "");
        mLevel = PreferenceManager.getDefaultSharedPreferences(this).getInt(JournalApplication.PREFERENCE_KEY_LEVEL, 0);
        //mAvatar = "404";
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
                new String[]{"События", "Журнал", "Расписание", "Классы", "Настройки", "О программе"}
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
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_layout, mCurrentFragment, mCurrentFragmentTag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();
    }

    private void completeLogout() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .remove(JournalApplication.PREFERENCE_KEY_TOKEN)
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
    public void onResponse(int actionCode, Object response) {
        if (actionCode != -1) {
            if (actionCode == APICodes.ACTION_LOGOUT) {
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
            }
        }
    }
}
