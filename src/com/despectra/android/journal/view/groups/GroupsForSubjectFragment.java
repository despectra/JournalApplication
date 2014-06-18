package com.despectra.android.journal.view.groups;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.view.LinksFragment;
import com.despectra.android.journal.view.MultipleRemoteIdsCursorAdapter;

/**
 * Created by Андрей on 16.06.14.
 */
public class GroupsForSubjectFragment extends LinksFragment {
    public static final String TAG = "GroupsForSubjectFragment";

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(0, R.id.action_add, 0, "Привязать классы");
        item.setIcon(R.drawable.ic_action_new);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    protected void linkEntities(EntityIds linkingEntityIds, MultipleRemoteIdsCursorAdapter adapterWithCheckedIds) {
        mServiceHelperController.setGroupsOfTeachersSubject(mToken,
                linkingEntityIds,
                adapterWithCheckedIds.getCheckedIdsOfTable(Groups.TABLE),
                ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    protected EntityIds getLinkingEntityIdsFromArgs(Bundle arguments) {
        return EntityIds.fromBundle(arguments.getBundle("teacher_subject_id"));
    }

    @Override
    protected CursorLoader getLinkDialogCursorLoader() {
        return new CursorLoader(getActivity(),
                Groups.URI,
                new String[]{Groups._ID + " as _id",
                        Groups.REMOTE_ID,
                        Groups.ENTITY_STATUS,
                        Groups.FIELD_NAME},
                String.format("%s NOT IN (SELECT %s FROM %s WHERE %s = ? AND %s = 0) AND %s = 0",
                        "_id",
                        TSG._ID,
                        TSG.TABLE,
                        TSG.FIELD_TEACHER_SUBJECT_ID,
                        TSG.ENTITY_STATUS,
                        Groups.ENTITY_STATUS),
                new String[]{String.valueOf(mLinkingEntityIds.getLocalId())},
                Groups.FIELD_NAME + " ASC");
    }

    @Override
    protected String getLinkDialogTag() {
        return "SetGroupsForTeachersSubjectDialog";
    }

    @Override
    protected String getLinkDialogTitle() {
        return "Привязать классы";
    }

    @Override
    protected Uri getLoaderUri() {
        return TSG.URI_WITH_GROUPS;
    }

    @Override
    protected String[] getLoaderProjection() {
        return new String[]{
                TSG._ID + " as _id",
                TSG.REMOTE_ID,
                TSG.ENTITY_STATUS,
                Groups.FIELD_NAME,
                Groups._ID,
                Groups.REMOTE_ID};
    }

    @Override
    protected String getLoaderSelection() {
        return TSG.FIELD_TEACHER_SUBJECT_ID + " = ?";
    }

    @Override
    protected String getLoaderOrderBy() {
        return Groups.FIELD_NAME + " ASC";
    }

    @Override
    protected String getUnlinkingConfirmTitle() {
        return "Отвязка классов";
    }

    @Override
    protected String getUnlinkingConfirmMessage() {
        return "Все связи класса с предметом будут удалены. Продолжить?";
    }

    @Override
    protected String getLinksEntitiesTable() {
        return TSG.TABLE;
    }

    @Override
    protected void unlinkEntities(EntityIds linkingEntityIds, EntityIds[] linkedEntitiesIds) {
        mServiceHelperController.unsetGroupsOfTeachersSubject(mToken, linkedEntitiesIds, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter getLinkDialogAdapter() {
        return new MultipleRemoteIdsCursorAdapter(getActivity(),
                R.layout.item_checkable_1,
                mCursor,
                new String[]{Groups.FIELD_NAME},
                new int[]{R.id.text1},
                new EntityIdsColumns[]{new EntityIdsColumns(Groups.TABLE, "_id", Groups.REMOTE_ID)},
                Groups.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.item_popup_menu_btn1,
                0);
    }

    @Override
    protected String getEmptyListMessage() {
        return "Учитель не ведет этот предмет ни у какого из классов. Добавьте связи с помощью кнопки +";
    }

    @Override
    public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
        //TODO
    }

    @Override
    public int getActionModeMenuRes() {
        return R.menu.groups_fragment_cab_menu;
    }

    @Override
    protected int getItemPopupMenuRes() {
        return R.menu.item_edit_del_menu;
    }

    @Override
    protected MultipleRemoteIdsCursorAdapter getRemoteIdAdapter() {
        EntityIdsColumns[] columns = new EntityIdsColumns[]{
            new EntityIdsColumns(TSG.TABLE, "_id", TSG.REMOTE_ID),
            new EntityIdsColumns(Groups.TABLE, Groups._ID, Groups.REMOTE_ID)
        };
        return new MultipleRemoteIdsCursorAdapter(getActivity(),
                R.layout.item_checkable_1,
                mCursor,
                new String[]{Groups.FIELD_NAME},
                new int[]{R.id.text1},
                columns,
                TSG.ENTITY_STATUS,
                R.id.checkbox1,
                R.id.item_popup_menu_btn1,
                0);
    }

    @Override
    protected String getConfirmDelDialogTag() {
        return "ConfirmUnsetGroupsForSubjectDialog";
    }

    @Override
    protected void updateEntitiesList() {
        mServiceHelperController.getAllGroups(mToken, new EntityIds(0, 0), ApiServiceHelper.PRIORITY_HIGH);
        mServiceHelperController.getGroupsOfTeachersSubject(mToken, mLinkingEntityIds, ApiServiceHelper.PRIORITY_LOW);
    }


}
