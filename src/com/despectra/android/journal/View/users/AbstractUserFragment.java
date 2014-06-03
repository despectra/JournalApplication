package com.despectra.android.journal.view.users;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import com.despectra.android.journal.JournalApplication;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.AbstractApiFragment;
import com.despectra.android.journal.view.EntitiesListFragment;
import com.despectra.android.journal.view.MultipleRemoteIdsCursorAdapter;
import com.despectra.android.journal.view.SimpleConfirmationDialog;
import com.despectra.android.journal.view.main_page.MainPageFragment;
import org.json.JSONObject;

/**
 * Created by Dmitry on 01.06.14.
 */
public abstract class AbstractUserFragment extends AbstractApiFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    protected String mToken;
    protected JoinedEntityIds mUserIds;
    private TextView mFirstNameView;
    private TextView mSndNameView;
    private TextView mMiddleNameView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mToken = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        mUserIds = JoinedEntityIds.fromBundle(args.getBundle("userId"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);
        mFirstNameView = (TextView) view.findViewById(R.id.user_first_name);
        mSndNameView = (TextView) view.findViewById(R.id.user_second_name);
        mMiddleNameView = (TextView) view.findViewById(R.id.user_middlename);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
        updateUserInfo();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected abstract void updateUserInfo();

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == 0) {
            return new CursorLoader(getActivity(),
                    Contract.Users.URI,
                    new String[]{Contract.Users.FIELD_NAME, Contract.Users.FIELD_SURNAME,
                            Contract.Users.FIELD_MIDDLENAME, Contract.Users.FIELD_LEVEL},
                    Contract.Users._ID + " = ?",
                    new String[]{String.valueOf(mUserIds.getIdsByTable("users").getLocalId())},
                    null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.getCount() <= 0) {
            Toast.makeText(getActivity(), "OMG, No user info in LOCAL", Toast.LENGTH_LONG).show();
            return;
        }
        cursor.moveToFirst();
        mFirstNameView.setText(cursor.getString(0));
        mSndNameView.setText(cursor.getString(1));
        mMiddleNameView.setText(cursor.getString(2));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {

    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        JSONObject jsonResponse = (JSONObject)response;
        int code = ApiErrorResponder.getErrorCode(jsonResponse);
        switch (code) {
            case 100:
                ApiErrorResponder.respondToast(getActivity(), jsonResponse);
                getActivity().finish();
                break;
        }
    }
}
