package com.despectra.android.journal.view.subjects;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.view.*;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.helper.ApiServiceHelper;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.view.LinksFragment;
import com.despectra.android.journal.view.MultipleRemoteIdsCursorAdapter;
import com.despectra.android.journal.view.groups.GroupsForSubjectFragment;

/**
 * Created by Dmitry on 04.06.14.
 */
public class SubjectsOfTeacherFragment extends LinksFragment {

    @Override
    protected String getEmptyListMessage() {
        return "Учитель еще не ведет ни одного предмета";
    }

    @Override
    public void onItemClick(View itemView, JoinedEntityIds ids) {
        //TODO TEST
        Bundle args = new Bundle();
        args.putBundle("teacher_subject_id", ids.getIdsByTable("teacher_subject").toBundle());

        //pas it to parent fragment

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_subj_of_teacher_menu, menu);
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
                R.id.item_popup_menu_btn1,
                0);
    }

    @Override
    protected String getConfirmDelDialogTag() {
        return "ConfirmUnsetSubjects";
    }

    @Override
    protected void updateEntitiesList() {
        mServiceHelperController.getSubjects(mToken, 0, 0, ApiServiceHelper.PRIORITY_HIGH);
        mServiceHelperController.getSubjectsOfTeacher(mToken, mLinkingEntityIds, ApiServiceHelper.PRIORITY_LOW);
    }

    @Override
    protected void linkEntities(EntityIds linkingEntityIds, EntityIds[] linkedEntitiesIds) {
        mServiceHelperController.setSubjectsOfTeacher(mToken, linkingEntityIds, linkedEntitiesIds, ApiServiceHelper.PRIORITY_HIGH);
    }

    @Override
    protected EntityIds getLinkingEntityIdsFromArgs(Bundle arguments) {
        return JoinedEntityIds.fromBundle(arguments.getBundle("userId")).getIdsByTable("teachers");
    }

    @Override
    protected CursorLoader getLinkDialogCursorLoader() {
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
    protected String getLinkDialogTag() {
        return "SetSubjectsOfTeacherDialog";
    }

    @Override
    protected String getLinkDialogTitle() {
        return "Добавление предметов учителя";
    }

    @Override
    protected Uri getLoaderUri() {
        return Contract.TeachersSubjects.URI_WITH_SUBJECTS;
    }

    @Override
    protected String[] getLoaderProjection() {
        return new String[]{
                Contract.TeachersSubjects._ID + " AS _id",
                Contract.TeachersSubjects.REMOTE_ID,
                Contract.Subjects._ID,
                Contract.Subjects.FIELD_NAME,
                Contract.Subjects.REMOTE_ID,
                Contract.TeachersSubjects.ENTITY_STATUS
        };
    }

    @Override
    protected String getLoaderSelection() {
        return Contract.TeachersSubjects.FIELD_TEACHER_ID + " = ?";
    }

    @Override
    protected String getLoaderOrderBy() {
        return Contract.Subjects.FIELD_NAME + " ASC";
    }

    @Override
    protected String getUnlinkingConfirmTitle() {
        return "Отвязка предметов от учителя";
    }

    @Override
    protected String getUnlinkingConfirmMessage() {
        return "Удалить связь этого предмета с учителем? Также удалятся все ячейки расписания, уроки, оценки, связанные с этим";
    }

    @Override
    protected String getLinksEntitiesTable() {
        return "teachers_subjects";
    }

    @Override
    protected void unlinkEntities(EntityIds linkingEntityIds, EntityIds[] linkedEntitiesIds) {
        mServiceHelperController.unsetSubjectsOfTeacher(mToken, linkedEntitiesIds, ApiServiceHelper.PRIORITY_HIGH);
    }
}