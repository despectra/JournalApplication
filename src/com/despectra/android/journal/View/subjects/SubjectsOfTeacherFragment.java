package com.despectra.android.journal.view.subjects;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.*;
import android.widget.ListView;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.ApiErrorResponder;
import com.despectra.android.journal.view.EntitiesListFragment;
import com.despectra.android.journal.view.MultipleRemoteIdsCursorAdapter;
import com.despectra.android.journal.view.SimpleConfirmationDialog;
import org.json.JSONObject;

/**
 * Created by Dmitry on 04.06.14.
 */
public class SubjectsOfTeacherFragment extends EntitiesListFragment {

    private static final String CONFIRM_DIALOG_TAG = "ConfirmUnsetSubjects";
    private EntityIds mTeacherIds;
    private AvailableSubjectsDialog mSubjectsSelectDialog;
    private AvailableSubjectsDialog.DialogListener mSubjectsSelectListener = new AvailableSubjectsDialog.DialogListener() {
        @Override
        public void onNewSubjectsPushed(EntityIds[] ids) {
            mServiceHelperController.setSubjectsOfTeacher(mToken, mTeacherIds, ids, ApiServiceHelper.PRIORITY_LOW);
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        JoinedEntityIds allIds = JoinedEntityIds.fromBundle(args.getBundle("userId"));
        mTeacherIds = allIds.getIdsByTable("teachers");
    }

    @Override
    protected String getTitle() {
        return null;
    }

    @Override
    protected void restoreCustom() {
        mSubjectsSelectDialog = (AvailableSubjectsDialog) getChildFragmentManager().findFragmentByTag(AvailableSubjectsDialog.FRAGMENT_TAG);
        if (mSubjectsSelectDialog == null) {
            mSubjectsSelectDialog = AvailableSubjectsDialog.newInstance(mTeacherIds);
        }
        mSubjectsSelectDialog.setListener(mSubjectsSelectListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_subj_of_teacher_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (mSubjectsSelectDialog == null) {
                    mSubjectsSelectDialog = AvailableSubjectsDialog.newInstance(mTeacherIds);
                    mSubjectsSelectDialog.setListener(mSubjectsSelectListener);
                }
                mSubjectsSelectDialog.show(getChildFragmentManager(), AvailableSubjectsDialog.FRAGMENT_TAG);
                return true;
            default:
                return false;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(
                getActivity(),
                Contract.TeachersSubjects.URI_WITH_SUBJECTS,
                new String[]{Contract.TeachersSubjects._ID + " AS _id",
                        Contract.TeachersSubjects.REMOTE_ID,
                        Contract.Subjects._ID,
                        Contract.Subjects.REMOTE_ID,
                        Contract.Subjects.FIELD_NAME,
                        Contract.TeachersSubjects.ENTITY_STATUS
                },
                Contract.TeachersSubjects.FIELD_TEACHER_ID + " = ?",
                new String[]{String.valueOf(mTeacherIds.getLocalId())},
                Contract.Subjects.FIELD_NAME + " ASC"
                );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        super.onLoadFinished(cursorLoader, cursor);
    }

    @Override
    protected String getEmptyListMessage() {
        return "Этот учитель не ведет ни одного предмета. Свяжите его с предметами с помощью кнопки на панели действий";
    }

    @Override
    public void onItemClick(View itemView, JoinedEntityIds ids) {
        //TODO goto groups for subject of this teacher
    }

    @Override
    public int getActionModeMenuRes() {
        return R.menu.groups_fragment_cab_menu;
    }

    @Override
    public boolean onActionModeItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                SimpleConfirmationDialog confirmDialog = SimpleConfirmationDialog.newInstance(
                        "Отвязка предметов",
                        "Внимание! Удалятся не связи с предметами, но и связанные уроки, оценки. Продолжить?");
                confirmDialog.setOnConfirmListener(getConfirmDelListener());
                confirmDialog.show(getFragmentManager(), CONFIRM_DIALOG_TAG);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter.OnItemPopupMenuListener getItemPopupMenuListener() {
        return null;
    }

    @Override
    protected int getItemPopupMenuRes() {
        return R.menu.fragment_subj_of_teacher_menu;
    }

    @Override
    protected int getFragmentLayoutRes() {
        return R.layout.fragment_simple_entities_list;
    }

    @Override
    protected int getListViewId() {
        return R.id.entities_list_view;
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter getRemoteIdAdapter() {
        EntityIdsColumns[] columns = new EntityIdsColumns[]{
            new EntityIdsColumns(Contract.TeachersSubjects.TABLE,
                    "_id",
                    Contract.TeachersSubjects.REMOTE_ID),
            new EntityIdsColumns(Contract.Subjects.TABLE,
                    Contract.Subjects._ID,
                    Contract.Subjects.REMOTE_ID)
        };
        return new MultipleRemoteIdsCursorAdapter(getActivity(),
                R.layout.item_checkable_1,
                mCursor,
                new String[]{Contract.Subjects.FIELD_NAME},
                new int[]{R.id.text1},
                columns,
                Contract.TeachersSubjects.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.dropdown_btn1,
                0);
    }

    @Override
    protected SimpleConfirmationDialog.OnConfirmListener getConfirmDelListener() {
        return new SimpleConfirmationDialog.OnConfirmListener() {
            @Override
            public void onConfirm() {
                mServiceHelperController.unsetSubjectsOfTeacher(mToken, mEntitiesAdapter.getCheckedIdsOfTable("teachers_subjects"),
                        ApiServiceHelper.PRIORITY_LOW);
                if (mIsInActionMode) {
                    mActionMode.finish();
                }
            }
        };
    }

    @Override
    protected String getConfirmDelDialogTag() {
        return CONFIRM_DIALOG_TAG;
    }

    @Override
    protected void notifyAboutRunningActions(int runningCount) {

    }

    @Override
    protected void updateEntitiesList() {
        mServiceHelperController.getSubjects(mToken, 0, 0, ApiServiceHelper.PRIORITY_HIGH);
        mServiceHelperController.getSubjectsOfTeacher(mToken, mTeacherIds, ApiServiceHelper.PRIORITY_LOW);
    }

    @Override
    protected void onResponseSuccess(int actionCode, int remainingActions, Object response) {

    }

    @Override
    protected void onResponseError(int actionCode, int remainingActions, Object response) {
        ApiErrorResponder.respondDialog(getChildFragmentManager(), (JSONObject)response);
    }

    /**
     * Created by Dmitry on 04.06.14.
     */
    public static class AvailableSubjectsDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

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
}
