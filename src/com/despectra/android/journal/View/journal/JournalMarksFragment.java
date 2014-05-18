package com.despectra.android.journal.view.journal;

import android.app.Activity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;
import com.despectra.android.journal.Adapters.HorizontalViewsRowAdapter;
import com.despectra.android.journal.Adapters.MarksRowAdapter;
import com.despectra.android.journal.Data.Contract;
import com.despectra.android.journal.Dialogs.AddEditDialog;
import com.despectra.android.journal.Dialogs.AddEditMark;
import com.despectra.android.journal.R;
import com.despectra.android.journal.view.fragments.AbstractApiFragment;

import java.util.Random;

/**
 * Created by Dmirty on 17.02.14.
 */
public class JournalMarksFragment extends AbstractApiFragment implements LoaderManager.LoaderCallbacks<Cursor>,HorizontalViewsRowAdapter.OnItemClickListener {
    public static final String ARG_POSITION = "pos";
    public static final String ARG_OFFSET = "offset";
    public static final String ARG_INDEX = "index";
    private static final String TAG = "JOURNAL_MARKS_FRAG";

    private JournalFragmentCallback mFragmentCallback;
    private GridView mHeaderGrid;
    private ListView mMarksGrid;
    private int mIndex;
    private long[] mStudentIds;
    private long mGroupId;
    private MarksRowAdapter mMarksAdapter;
    private AddEditMark mMarkDialog;
    private AddEditDialog.DialogButtonsListener mMarkDialogListener = new AddEditDialog.DialogButtonsAdapter() {
        @Override
        public void onPositiveClicked(int mode, Object... args) {
            super.onPositiveClicked(mode, args);
            long markId = (Long)args[0];
            int mark = (Integer)args[1];

            mServiceHelperController.updateMockMark(markId, mark);

            boolean dontClose = (Boolean)args[2];
            if (dontClose) {
                if (mMarksCount >= markId + 6) {
                    mMarkDialog.setData(markId + 6, -1);
                }
            }
        }
    };
    private int mMarksCount;

    public JournalMarksFragment() {
        super();
    }

    public int getIndex() {
        return mIndex;
    }

    public ListView getMarksGridView() {
        return  mMarksGrid;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        JournalFragment parentFragment = (JournalFragment) getFragmentManager().findFragmentByTag(JournalFragment.FRAGMENT_TAG);
        mGroupId = parentFragment.getGroupId();
        mFragmentCallback = parentFragment;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentCallback = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIndex = getArguments().getInt(ARG_INDEX);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApplicationContext.getApiServiceHelper().registerClient(this, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mApplicationContext.getApiServiceHelper().unregisterClient(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_journal, container, false);
        mHeaderGrid = (GridView)v.findViewById(R.id.journal_header_view);
        mMarksGrid = (ListView)v.findViewById(R.id.journal_marks_view);

        String[] headerData = new String[] {
                "10.04",
                "15.04",
                "16.04",
                "19.04",
                "21.04",
                "23.04"
        };
        MarksRowAdapter.MarkHolder[] marksData = new MarksRowAdapter.MarkHolder[120];
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < marksData.length; i++) {
            marksData[i] = new MarksRowAdapter.MarkHolder();
            marksData[i].localId = 0;
            marksData[i].remoteId = 0;
            marksData[i].mark = String.valueOf(i);
        }
        mMarksAdapter = new MarksRowAdapter(getActivity(), 6, new MarksRowAdapter.MarkHolder[]{});
        mMarksAdapter.setOnItemClickListener(this);
        mHeaderGrid.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.journal_day_item, headerData));
        mMarksGrid.setAdapter(mMarksAdapter);
        if (mFragmentCallback != null) {
            mFragmentCallback.onFragmentCreated(mIndex);
        }
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMarkDialog = (AddEditMark) getFragmentManager().findFragmentByTag(AddEditMark.FRAGMENT_TAG);
        if (mMarkDialog != null) {
            mMarkDialog.setDialogListener(mMarkDialogListener);
        }
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "JOURNAL MARKS FRAGMENT ONDESTROY");
    }

    public void setMarksGridScrolling(final int position, final int offset) {
        mMarksGrid.setSelectionFromTop(position, offset);
    }

    public void setGroupId(long id) {
        mGroupId = id;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader loader = new CursorLoader(getActivity(),
                Contract.Marks.URI_BY_GROUP,
                new String[]{Contract.Marks._ID, Contract.Marks.FIELD_MARK, Contract.Marks.ENTITY_STATUS},
                Contract.StudentsGroups.FIELD_GROUP_ID + " = " + mGroupId,
                null,
                Contract.Marks._ID + " ASC");
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.getCount() <= 0) {
            mMarksAdapter.swapMarks(new MarksRowAdapter.MarkHolder[]{});
            return;
        }
        mMarksCount = cursor.getCount();
        MarksRowAdapter.MarkHolder[] marksData = new MarksRowAdapter.MarkHolder[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        do {
            long markId = cursor.getLong(0);
            String mark = cursor.getString(1);
            int status = cursor.getInt(2);
            marksData[i] = new MarksRowAdapter.MarkHolder();
            marksData[i].localId = markId;
            marksData[i].status = status;
            marksData[i].mark = mark;

            i++;
        } while (cursor.moveToNext());
        mMarksAdapter.swapMarks(marksData);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    public void updateMarks() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onItemClick(int position, Object itemData) {
        MarksRowAdapter.MarkHolder mark = (MarksRowAdapter.MarkHolder) itemData;
        long markId = mark.localId;
        int initialMark = -1;
        mMarkDialog = AddEditMark.newInstance(markId, initialMark);
        mMarkDialog.setDialogListener(mMarkDialogListener);
        mMarkDialog.showInMode(AddEditDialog.MODE_ADD, getFragmentManager(), AddEditMark.FRAGMENT_TAG);
    }

    @Override
    public void onResponse(int actionCode, int remainingActions, Object response) {
        Toast.makeText(getActivity(), "Обновление завершено", Toast.LENGTH_SHORT).show();
    }

    public interface JournalFragmentCallback {
        public void onFragmentCreated(int index);
    }
}
