package com.despectra.android.journal.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.ApiErrorResponder;
import org.json.JSONObject;

/**
 * Created by Dmitry on 04.06.14.
 */
public abstract class LinksFragment extends EntitiesListFragment {

    private EntityIds mLinkingEntityIds;
    private AvailableEntitiesDialog mEntitiesSelectDialog;
    private AvailableEntitiesDialog.DialogListener mEntitiesSelectListener = new AvailableEntitiesDialog.DialogListener() {
        @Override
        public void onNewEntitiesPushed(EntityIds[] ids) {
            linkEntities(mLinkingEntityIds, ids);
        }
    };

    protected abstract void linkEntities(EntityIds linkingEntityIds, EntityIds[] linkedEntitiesIds);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLinkingEntityIds = getLinkingEntityIdsFromArgs(getArguments());
    }

    protected abstract EntityIds getLinkingEntityIdsFromArgs(Bundle arguments);

    @Override
    protected String getTitle() {
        return null;
    }

    @Override
    protected void restoreCustom() {
        mEntitiesSelectDialog = (AvailableEntitiesDialog) getChildFragmentManager().findFragmentByTag(getLinkDialogTag());
        if (mEntitiesSelectDialog == null) {
            mEntitiesSelectDialog = AvailableEntitiesDialog.newInstance(getLinkDialogTitle(), mLinkingEntityIds);
        }
        mEntitiesSelectDialog.setListener(mEntitiesSelectListener);
        mEntitiesSelectDialog.setLoaderCallbacks(getLinkDialogLoaderCallbacks());
    }

    protected abstract LoaderCallbacks<Cursor> getLinkDialogLoaderCallbacks();

    protected abstract String getLinkDialogTag();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (mEntitiesSelectDialog == null) {
                    mEntitiesSelectDialog = AvailableEntitiesDialog.newInstance(getLinkDialogTitle(), mLinkingEntityIds);
                    mEntitiesSelectDialog.setListener(mEntitiesSelectListener);
                    mEntitiesSelectDialog.setLoaderCallbacks(getLinkDialogLoaderCallbacks());
                }
                mEntitiesSelectDialog.show(getChildFragmentManager(), getLinkDialogTag());
                return true;
            default:
                return false;
        }
    }

    protected abstract String getLinkDialogTitle();

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return new CursorLoader(
                getActivity(),
                getLoaderUri(),
                getLoaderProjection(),
                getLoaderSelection(),
                new String[]{String.valueOf(mLinkingEntityIds.getLocalId())},
                getLoaderOrderBy()
        );
    }

    protected abstract Uri getLoaderUri();

    protected abstract String[] getLoaderProjection();

    protected abstract String getLoaderSelection();

    protected abstract String getLoaderOrderBy();

    @Override
    public boolean onActionModeItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                SimpleConfirmationDialog confirmDialog = SimpleConfirmationDialog.newInstance(
                        getUnlinkingConfirmTitle(),
                        getUnlinkingConfirmMessage());
                confirmDialog.setOnConfirmListener(getConfirmDelListener());
                confirmDialog.show(getFragmentManager(), getConfirmDelDialogTag());
                return true;
            default:
                return false;
        }
    }

    protected abstract String getUnlinkingConfirmTitle();

    protected abstract String getUnlinkingConfirmMessage();

    @Override
    protected MultipleRemoteIdsCursorAdapter.OnItemPopupMenuListener getItemPopupMenuListener() {
        return null;
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
    protected SimpleConfirmationDialog.OnConfirmListener getConfirmDelListener() {
        return new SimpleConfirmationDialog.OnConfirmListener() {
            @Override
            public void onConfirm() {
                unlinkEntities(mLinkingEntityIds, mEntitiesAdapter.getCheckedIds());
                if (mIsInActionMode) {
                    mActionMode.finish();
                }
            }
        };
    }

    protected abstract void unlinkEntities(EntityIds linkingEntityIds, JoinedEntityIds[] linkedEntitiesIds);

    @Override
    protected void notifyAboutRunningActions(int runningCount) {

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
    public static class AvailableEntitiesDialog extends DialogFragment implements LoaderCallbacks<Cursor> {

        private EntityIds mLinkingEntityIds;
        private ListView mListView;
        private TextView mEmptyTextView;
        private MultipleRemoteIdsCursorAdapter mAdapter;
        private Cursor mCursor;
        private DialogListener mListener;
        private LoaderCallbacks<Cursor> mLoaderCallbacks;
        private String mTitle;

        public static AvailableEntitiesDialog newInstance(String  title, EntityIds teacherIds) {
            AvailableEntitiesDialog dialog = new AvailableEntitiesDialog();
            Bundle args = new Bundle();
            args.putBundle("entityIds", teacherIds.toBundle());
            args.putString("title", title);
            dialog.setArguments(args);
            return dialog;
        }

        public void setListener(DialogListener listener) {
            mListener = listener;
        }

        public void setLoaderCallbacks(LoaderCallbacks<Cursor> callbacks) {
            mLoaderCallbacks = callbacks;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            mLinkingEntityIds = EntityIds.fromBundle(args.getBundle("entityIds"));
            mTitle = args.getString("title");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setNegativeButton("Отмена", null);

            dialogBuilder.setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (mListener != null) {
                        mListener.onNewEntitiesPushed(mAdapter.getCheckedIdsOfTable("subjects"));
                    }
                    dialogInterface.dismiss();
                }
            });
            View view = View.inflate(getActivity(), R.layout.fragment_simple_entities_list, null);
            mListView = (ListView) view.findViewById(R.id.entities_list_view);
            mEmptyTextView = (TextView) view.findViewById(R.id.listview_empty_message);
            dialogBuilder.setView(view);
            dialogBuilder.setTitle(mTitle);
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
            getLoaderManager().initLoader(0, null, mLoaderCallbacks);
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
                    new String[]{String.valueOf(mLinkingEntityIds.getLocalId())},
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
            public void onNewEntitiesPushed(EntityIds[] ids);
        }
    }
}
