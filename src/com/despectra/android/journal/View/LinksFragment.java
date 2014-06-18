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
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.utils.ApiErrorResponder;
import org.json.JSONObject;

/**
 * Created by Dmitry on 04.06.14.
 */
public abstract class LinksFragment extends EntitiesListFragment {

    protected EntityIds mLinkingEntityIds;
    private AvailableEntitiesDialog mEntitiesSelectDialog;
    private AvailableEntitiesDialog.DialogListener mEntitiesSelectListener = new AvailableEntitiesDialog.DialogListener() {
        @Override
        public void onNewEntitiesPushed(MultipleRemoteIdsCursorAdapter adapter) {
            linkEntities(mLinkingEntityIds, adapter);
        }
    };

    private OnItemClickedListener mItemClickedListener;

    protected abstract void linkEntities(EntityIds linkingEntityIds, MultipleRemoteIdsCursorAdapter adapterWithCheckedIds);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLinkingEntityIds = getLinkingEntityIdsFromArgs(getArguments());
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mItemClickedListener = listener;
    }

    @Override
    public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
        if (mItemClickedListener != null) {
            mItemClickedListener.onItemClicked(mEntitiesListView, position, itemView, ids);
        }
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
        mEntitiesSelectDialog.setListAdapter(getLinkDialogAdapter());
        mEntitiesSelectDialog.setListener(mEntitiesSelectListener);
        mEntitiesSelectDialog.setCursorLoader(getLinkDialogCursorLoader());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    protected abstract CursorLoader getLinkDialogCursorLoader();

    protected abstract String getLinkDialogTag();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (mEntitiesSelectDialog == null) {
                    mEntitiesSelectDialog = AvailableEntitiesDialog.newInstance(getLinkDialogTitle(), mLinkingEntityIds);
                }
                mEntitiesSelectDialog.setListener(mEntitiesSelectListener);
                mEntitiesSelectDialog.setListAdapter(getLinkDialogAdapter());
                mEntitiesSelectDialog.setCursorLoader(getLinkDialogCursorLoader());
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
                unlinkEntities(mLinkingEntityIds, mEntitiesAdapter.getCheckedIdsOfTable(getLinksEntitiesTable()));
                if (mIsInActionMode) {
                    mActionMode.finish();
                }
            }
        };
    }

    protected abstract String getLinksEntitiesTable();

    protected abstract void unlinkEntities(EntityIds linkingEntityIds, EntityIds[] linkedEntitiesIds);

    protected abstract MultipleRemoteIdsCursorAdapter getLinkDialogAdapter();

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

    public static class AvailableEntitiesDialog extends DialogFragment implements LoaderCallbacks<Cursor> {
        private EntityIds mLinkingEntityIds;
        private ListView mListView;
        private TextView mEmptyTextView;
        private MultipleRemoteIdsCursorAdapter mAdapter;
        private Cursor mCursor;
        private DialogListener mListener;
        private CursorLoader mCursorLoader;

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

        public void setCursorLoader(CursorLoader loader) {
            mCursorLoader = loader;
        }

        public void setListAdapter(MultipleRemoteIdsCursorAdapter adapter) {
            mAdapter = adapter;
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
                        mListener.onNewEntitiesPushed(mAdapter);
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
            if (savedInstanceState != null) {
                mAdapter.restoreCheckedItems(savedInstanceState.getBundle("selectedEntities"), false);
            }
            mListView.setAdapter(mAdapter);
            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBundle("selectedEntities", mAdapter.getCheckedItems());
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return mCursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            mAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }

        public interface DialogListener {
            public void onNewEntitiesPushed(MultipleRemoteIdsCursorAdapter adapter);
        }
    }

    public interface OnItemClickedListener {
        public void onItemClicked(ListView listView, int position, View clickedItemView, JoinedEntityIds ids);
    }
}
