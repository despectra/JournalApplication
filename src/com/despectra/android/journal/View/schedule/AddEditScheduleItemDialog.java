package com.despectra.android.journal.view.schedule;

import android.app.AlertDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.local.Contract.*;
import com.despectra.android.journal.model.EntityIds;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.model.JoinedEntityIds;
import com.despectra.android.journal.model.WeekScheduleItem;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.AddEditDialog;
import com.despectra.android.journal.view.RemoteIdsCursorAdapter;
import com.despectra.android.journal.view.SimpleRemoteIdsAdapter;

/**
 * Created by Dmitry on 27.06.14.
 */
public class AddEditScheduleItemDialog extends AddEditDialog implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener {

    public static final String TAG = "AddEditScheduleDialog";

    public static final int LOADER_TEACHERS = 0;
    public static final int LOADER_SUBJECTS = 1;
    public static final int LOADER_TSG = 2;

    private Spinner mSelectTeacherSpinner;
    private Spinner mSelectSubjectSpinner;
    private SimpleRemoteIdsAdapter mTeachersAdapter;
    private SimpleRemoteIdsAdapter mSubjectsAdapter;
    private Cursor mTeachersCursor;
    private Cursor mSubjectsCursor;
    private RemoteIdsCursorAdapter.OnItemClickListener mTeacherSelectedListener = new RemoteIdsCursorAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
            mSelectedTeacherIds = ids.getIdsByTable(Teachers.TABLE);
            getLoaderManager().restartLoader(LOADER_SUBJECTS, null, AddEditScheduleItemDialog.this);
        }
    };
    private RemoteIdsCursorAdapter.OnItemClickListener mSubjectSelectedListener = new RemoteIdsCursorAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View itemView, int position, JoinedEntityIds ids) {
            mSelectedTeacherSubjectIds = ids.getIdsByTable(TeachersSubjects.TABLE);
        }
    };

    private EntityIds mSelectedTeacherIds;
    private EntityIds mSelectedTeacherSubjectIds;
    private EntityIds mSelectedTSGIds;
    private DialogListener mDialogListener;

    public static AddEditScheduleItemDialog newInstance(FragmentManager fm, int day, int lessonNum, EntityIds groupIds,
                                                        WeekScheduleItem scheduleItem, String addTitle, String editTitle) {
        AddEditScheduleItemDialog dialog = (AddEditScheduleItemDialog)fm.findFragmentByTag(TAG);
        if (dialog == null) {
            dialog = new AddEditScheduleItemDialog();
        }
        dialog.prepareAllArguments(R.layout.dialog_add_schedule_item,
                addTitle,
                editTitle,
                "Отмена",
                "Добавить",
                "Обновить",
                "",
                new ScheduleItemDialogData(day, lessonNum, groupIds, scheduleItem));
        return dialog;
    }

    public void setDialogListener(DialogListener listener) {
        mDialogListener = listener;
    }

    @Override
    protected void completeDialogCreation(AlertDialog.Builder builder) {
        mSelectTeacherSpinner = (Spinner) mMainView.findViewById(R.id.select_teacher);
        mSelectSubjectSpinner = (Spinner) mMainView.findViewById(R.id.select_subject);
        mTeachersAdapter = new SimpleRemoteIdsAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                mTeachersCursor,
                new EntityIdsColumns[]{
                        new EntityIdsColumns(Teachers.TABLE, "_id", Teachers.REMOTE_ID)
                },
                Teachers.ENTITY_STATUS,
                new String[]{"teacher_first_last_name"},
                new int[]{android.R.id.text1},
                RemoteIdsCursorAdapter.FLAG_SPINNER_ADAPTER);
        mSubjectsAdapter = new SimpleRemoteIdsAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                mSubjectsCursor,
                new EntityIdsColumns[]{
                        new EntityIdsColumns(TeachersSubjects.TABLE, "_id", TeachersSubjects.REMOTE_ID)
                },
                TeachersSubjects.ENTITY_STATUS,
                new String[]{Subjects.FIELD_NAME},
                new int[]{android.R.id.text1},
                RemoteIdsCursorAdapter.FLAG_SPINNER_ADAPTER);
        mSelectTeacherSpinner.setAdapter(mTeachersAdapter);
        mSelectSubjectSpinner.setAdapter(mSubjectsAdapter);
        mSelectTeacherSpinner.setPrompt("Выбрать учителя");
        mSelectSubjectSpinner.setPrompt("Выбрать предмет");
        mSelectTeacherSpinner.setOnItemSelectedListener(this);
        mSelectSubjectSpinner.setOnItemSelectedListener(this);

        ScheduleItemDialogData data = (ScheduleItemDialogData) mDialogData;
        if (data.scheduleItem != null) {
            mSelectedTeacherIds = data.scheduleItem.scheduleIds.getIdsByTable(Teachers.TABLE);
            mSelectedTeacherSubjectIds = data.scheduleItem.scheduleIds.getIdsByTable(TeachersSubjects.TABLE);
        }
        builder.setView(mMainView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(LOADER_TEACHERS, null, this);
    }

    @Override
    protected void clearDialogView() {
    }

    @Override
    protected void respondNotValidated() {
        Toast.makeText(getActivity(), "Выбраны не все элементы", Toast.LENGTH_LONG).show();
    }

    @Override
    protected boolean validateInputData() {
        return mSelectTeacherSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION &&
                mSelectSubjectSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = null;
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String orderBy = null;
        ScheduleItemDialogData data = (ScheduleItemDialogData) mDialogData;
        switch (i) {
            case LOADER_TEACHERS:
                uri = Teachers.URI_WITH_TSG;
                projection = new String[]{"DISTINCT " + Teachers._ID + " as _id", Teachers.REMOTE_ID, Teachers.ENTITY_STATUS,
                        String.format("%s||' '||%s||' '||%s as teacher_first_last_name",
                                Users.FIELD_LAST_NAME, Users.FIELD_FIRST_NAME, Users.FIELD_MIDDLE_NAME),};
                String busyTeachersSubquery = String.format("SELECT DISTINCT %s FROM %s WHERE %s = ? AND %s = ?",
                        Teachers._ID,
                        Schedule.TABLE_JOIN_FULL,
                        Schedule.FIELD_DAY,
                        Schedule.FIELD_LESSON_NUMBER);
                selection = String.format("%s = ? AND %s NOT IN (%s)",
                        TSG.FIELD_GROUP_ID,
                        Teachers._ID,
                        busyTeachersSubquery);
                selectionArgs = new String[]{
                        String.valueOf(data.groupIds.getLocalId()),
                        String.valueOf(data.day),
                        String.valueOf(data.lessonNum)
                };
                orderBy = "teacher_first_last_name ASC";
                break;
            case LOADER_SUBJECTS:
                uri = TSG.URI_WITH_SUBJECTS;
                projection = new String[]{"DISTINCT " + TeachersSubjects._ID + " as _id", TeachersSubjects.REMOTE_ID,
                        Subjects.FIELD_NAME, TeachersSubjects.ENTITY_STATUS};
                selection = String.format("%s = ? AND %s = ?", TeachersSubjects.FIELD_TEACHER_ID, TSG.FIELD_GROUP_ID );
                selectionArgs = new String[]{
                        String.valueOf(mSelectedTeacherIds.getLocalId()),
                        String.valueOf(data.groupIds.getLocalId())
                };
                orderBy = Subjects.FIELD_NAME + " ASC";
                break;
            case LOADER_TSG:
                uri = TSG.URI;
                projection = new String[]{TSG._ID, TSG.REMOTE_ID};
                selection = String.format("%s = ? AND %s = ?", TSG.FIELD_GROUP_ID, TSG.FIELD_TEACHER_SUBJECT_ID);
                selectionArgs = new String[]{
                        String.valueOf(data.groupIds.getLocalId()),
                        String.valueOf(mSelectedTeacherSubjectIds.getLocalId())
                };
                orderBy = null;
                break;
            default:
                return null;
        }
        return new CursorLoader(getActivity(), uri, projection, selection, selectionArgs, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            switch (cursorLoader.getId()) {
                case LOADER_TEACHERS:
                    mTeachersAdapter.swapCursor(cursor);
                    JoinedEntityIds teacherIds = mTeachersAdapter.getItemIds(0);
                    if (teacherIds != null) {
                        mSelectedTeacherIds = teacherIds.getIdsByTable(Teachers.TABLE);
                    }
                    break;
                case LOADER_SUBJECTS:
                    mSubjectsAdapter.swapCursor(cursor);
                    JoinedEntityIds teacherSubjectIds = mSubjectsAdapter.getItemIds(0);
                    if (teacherSubjectIds != null) {
                        mSelectedTeacherSubjectIds = teacherSubjectIds.getIdsByTable(TeachersSubjects.TABLE);
                    }
                    break;
                case LOADER_TSG:
                    cursor.moveToFirst();
                    mSelectedTSGIds = new EntityIds(cursor.getLong(0), cursor.getLong(1));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (adapterView.getId()) {
            case R.id.select_teacher:
                JoinedEntityIds teacherIds = mTeachersAdapter.getItemIds(pos);
                if (teacherIds != null) {
                    mSelectedTeacherIds = teacherIds.getIdsByTable(Teachers.TABLE);
                    getLoaderManager().restartLoader(LOADER_SUBJECTS, null, this);
                }
                break;
            case R.id.select_subject:
                JoinedEntityIds teacherSubjectIds = mSubjectsAdapter.getItemIds(pos);
                if (teacherSubjectIds != null) {
                    mSelectedTeacherSubjectIds = teacherSubjectIds.getIdsByTable(TeachersSubjects.TABLE);
                    getLoaderManager().restartLoader(LOADER_TSG, null, this);
                }
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Toast.makeText(getActivity(), adapterView.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onNeutralClicked(int mode) {
        ScheduleItemDialogData data = (ScheduleItemDialogData)mDialogData;
        switch (mode) {
            case MODE_ADD:
                if (mDialogListener != null) {
                    mDialogListener.onAddScheduleItem(mSelectedTSGIds, data.day, data.lessonNum, Utils.getRandomHoloColor(getActivity()));
                }
                break;
            case MODE_EDIT:
                if (mDialogListener != null) {
                    mDialogListener.onEditScheduleItem(data.scheduleItem.scheduleIds.getIdsByTable(Schedule.TABLE),
                            data.scheduleItem.scheduleIds.getIdsByTable(TSG.TABLE), mSelectedTSGIds);
                }
                break;
        }
    }

    public void setData(int day, int lessonNum, EntityIds selectedGroupIds, WeekScheduleItem scheduleItem) {
        mDialogData = new ScheduleItemDialogData(day, lessonNum, selectedGroupIds, scheduleItem);
    }

    public static final class ScheduleItemDialogData extends DialogData {

        public int day;
        public int lessonNum;
        public EntityIds groupIds;
        public WeekScheduleItem scheduleItem;

        public ScheduleItemDialogData(int day, int lessonNum, EntityIds groupIds, WeekScheduleItem scheduleItem) {
            this.day = day;
            this.lessonNum = lessonNum;
            this.groupIds = groupIds;
            this.scheduleItem = scheduleItem;
        }

        public ScheduleItemDialogData(Parcel parcel) {
            this.day = parcel.readInt();
            this.lessonNum = parcel.readInt();
            this.groupIds = EntityIds.fromBundle(parcel.readBundle());
            this.scheduleItem = new WeekScheduleItem(JoinedEntityIds.fromBundle(parcel.readBundle()),
                    parcel.readString(), parcel.readString(), parcel.readInt());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(day);
            parcel.writeInt(lessonNum);
            parcel.writeBundle(groupIds.toBundle());
            parcel.writeBundle(scheduleItem.scheduleIds.toBundle());
            parcel.writeString(scheduleItem.label1);
            parcel.writeString(scheduleItem.label2);
            parcel.writeInt(scheduleItem.color);
        }

        public final Creator<ScheduleItemDialogData> CREATOR = new Creator<ScheduleItemDialogData>() {
            @Override
            public ScheduleItemDialogData createFromParcel(Parcel parcel) {
                return new ScheduleItemDialogData(parcel);
            }

            @Override
            public ScheduleItemDialogData[] newArray(int i) {
                return new ScheduleItemDialogData[i];
            }
        };
    }

    public interface DialogListener {
        public void onAddScheduleItem(EntityIds tsgIds, int day, int lessonNum, int color);
        public void onEditScheduleItem(EntityIds scheduleItemIds, EntityIds oldTsgIds, EntityIds newTsgIds);
    }
}
