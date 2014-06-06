package com.despectra.android.journal.view.subjects;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.view.MultipleRemoteIdsCursorAdapter;
import com.despectra.android.journal.view.users.AddEditSimpleUserDialog;

/**
 * Created by Dmitry on 04.06.14.
 */
public class AvailableSubjectsDialog extends DialogFragment implements LoaderCallbacks<Cursor> {

    public static final String FRAGMENT_TAG = "AvailableSubjects";
    private EntityIds mLocalTeacherId;
    private ListView mListView;
    private TextView mEmptyTextView;
    private MultipleRemoteIdsCursorAdapter mAdapter;
    private Cursor mCursor;
    private DialogListener mListener;

    public static AvailableSubjectsDialog newInstance(EntityIds teacherIds) {
        AvailableSubjectsDialog dialog = new AvailableSubjectsDialog();
        Bundle args = new Bundle();
        args.putBundle("teacherIds", teacherIds.toBundle());
        dialog.setArguments(args);
        return dialog;
    }

    public void setListener(DialogListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mLocalTeacherId = EntityIds.fromBundle(args.getBundle("teacherIds"));
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setNegativeButton("Отмена", null);

        dialogBuilder.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (mListener != null) {
                    mListener.onNewSubjectsPushed(mAdapter.getCheckedIdsOfTable("subjects"));
                }
                dialogInterface.dismiss();
            }
        });
        View view = View.inflate(getActivity(), R.layout.fragment_simple_entities_list, null);
        mListView = (ListView) view.findViewById(R.id.entities_list_view);
        mEmptyTextView = (TextView) view.findViewById(R.id.listview_empty_message);
        dialogBuilder.setView(view);
        dialogBuilder.setTitle("Выбор предметов учителя");
        return dialogBuilder.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new MultipleRemoteIdsCursorAdapter(getActivity(),
                R.layout.item_checkable_1,
                mCursor,
                new String[]{Contract.Subjects.FIELD_NAME},
                new int[]{R.id.text1},
                new EntityIdsColumns[]{new EntityIdsColumns("subjects", "_id", Contract.Subjects.REMOTE_ID)},
                Contract.Subjects.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.dropdown_btn1,
                0);
        mListView.setAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(),
                Contract.Subjects.URI,
                new String[]{Contract.Subjects._ID + " AS _id",
                    Contract.Subjects.REMOTE_ID,
                    Contract.Subjects.FIELD_NAME,
                    Contract.Subjects.ENTITY_STATUS},
                String.format("%s NOT IN (SELECT %s FROM %s WHERE %s = ? AND %s = 0) AND %s = 0",
                        "_id",
                        Contract.TeachersSubjects.FIELD_SUBJECT_ID,
                        Contract.TeachersSubjects.TABLE,
                        Contract.TeachersSubjects.FIELD_TEACHER_ID,
                        Contract.TeachersSubjects.ENTITY_STATUS,
                        Contract.Subjects.ENTITY_STATUS
                        ),
                new String[]{String.valueOf(mLocalTeacherId.getLocalId())},
                Contract.Subjects.FIELD_NAME + " ASC"
                );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    public interface DialogListener {
        public void onNewSubjectsPushed(EntityIds[] ids);
    }

}
