package com.despectra.android.journal.Fragments;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.*;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.despectra.android.journal.Adapters.RemoteIdCursorAdapter;
import com.despectra.android.journal.App.JournalApplication;
import com.despectra.android.journal.Data.Contract;
import com.despectra.android.journal.Dialogs.AddEditDialog;
import com.despectra.android.journal.Dialogs.AddEditStudentDialog;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Server.APICodes;
import com.despectra.android.journal.Services.ApiServiceHelper;

/**
 * Created by Dmitry on 13.04.14.
 */
public class StudentsFragment extends AbstractApiFragment implements LoaderManager.LoaderCallbacks<Cursor>, ApiServiceHelper.FeedbackApiClient {

    public static final String FRAGMENT_TAG = "studentsFrag";

    public static final String KEY_LOCAL_GROUP_ID = "localgroupId";
    public static final String KEY_REMOTE_GROUP_ID="remotegroupId";

    private static final int LOADER_STUDENTS = 0;
    private ListView mStudentsListView;
    private RemoteIdCursorAdapter mStudentsAdapter;
    private AddEditStudentDialog mStudentDialog;
    private Cursor mCursor;
    private long mLocalGroupId;
    private long mRemoteGroupId;
    private String mToken;
    private boolean mLoadStudents;


    private AddEditDialog.DialogButtonsListener mStudentsDialogListener = new AddEditDialog.DialogButtonsAdapter() {
        @Override
        public void onPositiveClicked(int mode, Object... args) {
            String name = (String) args[1];
            String middlename = (String) args[2];
            String surname = (String) args[3];
            String login = (String) args[4];
            if (name.isEmpty() || middlename.isEmpty() || surname.isEmpty() || login.isEmpty()) {
                Toast.makeText(getActivity(), "Некоторые поля пустые", Toast.LENGTH_LONG).show();
                return;
            }
            if (mode == AddEditDialog.MODE_ADD) {
                String token = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
                if (token.isEmpty()) {
                    return;
                }
                mServiceHelperController.addStudentIntoGroup(token, mLocalGroupId, mRemoteGroupId, name, middlename, surname, login, ApiServiceHelper.PRIORITY_HIGH);
            } else {
                //TODO edit student
                long localId = (Long) args[0];
            }
        }
    };

    public StudentsFragment() {
        super();
    }

    public static StudentsFragment newInstance(long localGroupId, long remoteGroupId) {
        StudentsFragment fragment = new StudentsFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_LOCAL_GROUP_ID, localGroupId);
        args.putLong(KEY_REMOTE_GROUP_ID, remoteGroupId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mLocalGroupId = args.getLong(KEY_LOCAL_GROUP_ID);
        mRemoteGroupId = args.getLong(KEY_REMOTE_GROUP_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_students, container, false);
        mStudentsListView = (ListView) view.findViewById(R.id.students_view);
        mStudentsListView.setEmptyView(view.findViewById(R.id.listview_empty_message));
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLoadStudents = (savedInstanceState == null);
        mStudentsAdapter = new RemoteIdCursorAdapter(getActivity(),
                R.layout.item_student_1,
                mCursor,
                new String[]{Contract.Users.FIELD_SURNAME, Contract.Users.FIELD_NAME, Contract.Users.FIELD_MIDDLENAME},
                new int[]{R.id.surname_view, R.id.name_view, R.id.middlename_view},
                BaseColumns._ID,
                Contract.Users.Remote.REMOTE_ID,
                Contract.Users.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.dropdown_btn1,
                0);
        mStudentsListView.setAdapter(mStudentsAdapter);
        mToken = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(JournalApplication.PREFERENCE_KEY_TOKEN, "");
        getLoaderManager().initLoader(LOADER_STUDENTS, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
        if (mLoadStudents) {
            mLoadStudents = false;
            mServiceHelperController.getStudentsByGroup(mToken, mLocalGroupId, mRemoteGroupId, ApiServiceHelper.PRIORITY_LOW);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_students_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_student:
                if (mStudentDialog == null) {
                    mStudentDialog = AddEditStudentDialog.newInstance("Добавление ученика в класс", "Редактирование ученика", -1, "", "", "", "");
                }
                mStudentDialog.setDialogListener(mStudentsDialogListener);
                mStudentDialog.showInMode(AddEditDialog.MODE_ADD, getFragmentManager(), AddEditStudentDialog.TAG);
                break;
        }
        return true;
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {
        if (actionCode != -1) {
            switch (actionCode) {
                case APICodes.ACTION_GET_STUDENTS_BY_GROUP:
                    getLoaderManager().restartLoader(LOADER_STUDENTS, null, this);
                    break;
                case APICodes.ACTION_ADD_STUDENT_IN_GROUP:
                    //TODO notify
                    getLoaderManager().restartLoader(LOADER_STUDENTS, null, this);
                    break;
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri uri;
        String selection;
        String[] projection;
        String[] selectionArgs;
        String orderBy;
        switch (id) {
            case LOADER_STUDENTS:
                uri = Uri.parse(String.format("%s/groups_remote/%d/students_remote", Contract.STRING_URI, mLocalGroupId));
                selection = Contract.StudentsGroups.FIELD_GROUP_ID + " = ?";
                selectionArgs = new String[]{String.valueOf(mLocalGroupId)};
                projection = new String[]{Contract.Users._ID + " AS _id", Contract.Users.Remote.REMOTE_ID,
                        Contract.Users.FIELD_NAME, Contract.Users.FIELD_SURNAME, Contract.Users.FIELD_MIDDLENAME, Contract.Users.ENTITY_STATUS};
                orderBy = Contract.Users.FIELD_SURNAME + " ASC";
                break;
            default:
                return null;
        }
        return new CursorLoader(
                getActivity(),
                uri,
                projection,
                selection,
                selectionArgs,
                orderBy
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mStudentsAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mStudentsAdapter.swapCursor(null);
    }

    @Override
    public void onProgress(Object data) {
        if (data.equals("cached")) {
            getActivity().getLoaderManager().restartLoader(LOADER_STUDENTS, null, this);
        }
    }
}
