package com.despectra.android.journal.view.schedule;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.model.WeekScheduleItem;
import com.despectra.android.journal.utils.Utils;
import com.despectra.android.journal.view.PseudoGridAdapter;

/**
 * Created by Dmitry on 14.03.14.
 */
public class ScheduleRowAdapter extends PseudoGridAdapter<WeekScheduleItem> {
    private static final String TAG = "ScheduleRowAdapter";
    private static final int COLS_COUNT = 9;
    public static final int COL_NUM = 0;
    public static final int COL_TIME = 8;
    public static final int LESSON_NUM_ITEM_LAYOUT = R.layout.week_schedule_position_item;
    public static final int TIME_INTERVAL_ITEM_LAYOUT = R.layout.week_schedule_time_item;
    private static final int ITEM_LAYOUT = R.layout.week_schedule_item;
    public static final int ITEM_BACKGROUND_IDLE_BORDERS = R.drawable.border_left_bottom;
    public static final int ITEM_BACKGROUND_IDLE_CLICKABLE = R.drawable.schedule_item_background_selector;

    private OnScheduleItemClickedListener mListener;

    private final int mRowsCount;

    private int mLeftColumnWidth;
    private int mRightColumnWidth;

    private int mDataItemWidth;
    private int mDayColumnIndex;
    private int mLessonNumColumnIndex;
    private int mLocalIdColumnIndex;
    private int mRemoteIdColumnIndex;
    private int mGroupColumnIndex;
    private int mTeacherColumnIndex;
    private int mSubjectColumnIndex;
    private int mColorColumnIndex;

    public ScheduleRowAdapter(Context context, Cursor cursor, int rowsCount) {
        super(context, cursor, 9, 7, new int[]{50, 0, 0, 0, 0, 0, 0, 0, 50});
        mRowsCount = rowsCount;
    }

    public void recalculateColumnsWidths(int parentContainerWidth) {
        int[] widths = new int[COLS_COUNT];
        widths[COL_NUM] = 50;
        widths[COL_TIME] = 50;
        int dataColumnWidth = (parentContainerWidth - 100) / 7;
        for (int i = 1; i < widths.length - 1; i++) {
            widths[i] = dataColumnWidth;
        }
        setColumnsWidths(widths, false);
    }

    public void setScheduleItemClickedListener(OnScheduleItemClickedListener listener) {
        mListener = listener;
    }

    public void setLeftColumnWidth(int width) {
        mLeftColumnWidth = width;
        notifyDataSetChanged();
    }

    public void setRightColumnWidth(int width) {
        mRightColumnWidth = width;
        notifyDataSetChanged();
    }

    @Override
    protected boolean isDataColumn(int column) {
        return column >= 1 && column <= 7;
    }

    @Override
    protected View newNonDataColumn(int column) {
        View v;
        switch (column) {
            case COL_NUM:
                v = View.inflate(getContext(), LESSON_NUM_ITEM_LAYOUT, null);
                break;
            case COL_TIME:
                v= View.inflate(getContext(), TIME_INTERVAL_ITEM_LAYOUT, null);
                break;
            default:
                return null;
        }
        v.setBackground(getContext().getResources().getDrawable(ITEM_BACKGROUND_IDLE_BORDERS));
        return v;
    }

    @Override
    protected View newDataColumn(ViewGroup parent) {
        View v = View.inflate(getContext(), ITEM_LAYOUT, null);
        ViewHolder vh = new ViewHolder(
                (TextView) v.findViewById(R.id.schedule_item_subject),
                (TextView) v.findViewById(R.id.schedule_item_teacher)
        );
        v.setTag(vh);
        return v;
    }

    @Override
    protected void bindNonDataColumn(int row, int column, View cell) {
        switch (column) {
            case COL_NUM:
                TextView numView = (TextView) cell;
                numView.setText(String.valueOf(row + 1));
                break;
            case COL_TIME:
                TextView timeStartView = (TextView) cell.findViewById(R.id.schedule_time_start);
                TextView timeEndView = (TextView) cell.findViewById(R.id.schedule_time_end);
                timeStartView.setText("8:30");
                timeEndView.setText("10:05");
                break;
        }
    }

    @Override
    protected void bindDataColumn(int row, int column, View cell) {
        final int day = column;
        final int lessonNum = row + 1;
        final WeekScheduleItem item = mData.get(lessonNum, day);
        StateListDrawable background = new StateListDrawable();
        ViewHolder vh = (ViewHolder) cell.getTag();
        if (item != null) {
            vh.fill(item.label1, item.label2);
            background.addState(new int[]{android.R.attr.state_pressed},
                    getContext().getResources().getDrawable(R.drawable.mark_item_background));
            background.addState(new int[]{},
                    new LayerDrawable(
                            new Drawable[]{
                                    new ColorDrawable(item.color),
                                    getContext().getResources().getDrawable(R.drawable.border_left_bottom)
                            }
                    ));
        } else {
            background = (StateListDrawable) getContext().getResources().getDrawable(R.drawable.schedule_item_background_selector);
            vh.fill("", "");
        }
        cell.setBackground(background);
        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onItemClicked(day, lessonNum, item);
                }
            }
        });
    }

    public void setDataItemWidth(int width) {
        mDataItemWidth = width;
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        mDayColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule.FIELD_DAY);
        mLessonNumColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule.FIELD_LESSON_NUMBER);
        mLocalIdColumnIndex = c.getColumnIndexOrThrow("_id");
        mRemoteIdColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule.REMOTE_ID);
        mTeacherColumnIndex = c.getColumnIndexOrThrow("teacher");
        mSubjectColumnIndex = c.getColumnIndexOrThrow(Contract.Subjects.FIELD_NAME);
        mColorColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule.FIELD_COLOR);
        return super.swapCursor(c);
    }

    @Override
    protected void convertCursorItemToMatrixRow(Cursor cursor) {
        long day = cursor.getLong(mDayColumnIndex);
        long lessonNum = cursor.getLong(mLessonNumColumnIndex);
        mData.putCell(lessonNum, day,
                new WeekScheduleItem(cursor.getLong(mLocalIdColumnIndex),
                        cursor.getLong(mRemoteIdColumnIndex),
                        cursor.getString(mSubjectColumnIndex),
                        cursor.getString(mTeacherColumnIndex),
                        cursor.getInt(mColorColumnIndex)));
    }

    @Override
    public int getCount() {
        return mRowsCount;
    }

    public interface OnScheduleItemClickedListener {
        public void onItemClicked(int day, int lessonNum, WeekScheduleItem item);
    }

    public static class ViewHolder {
        public TextView subjectView;
        public TextView teacherView;
        public ViewHolder(TextView subjectView, TextView teacherView) {
            this.subjectView = subjectView;
            this.teacherView = teacherView;
        }

        public void fill(String subject, String teacher) {
            subjectView.setText(subject);
            teacherView.setText(teacher);
        }
    }
}
