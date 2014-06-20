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

    public ScheduleRowAdapter(Context context, int rowsCount) {
        super(context, 9, 7, new int[]{50, 0, 0, 0, 0, 0, 0, 0, 50});
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
        int day = column - 1;
        int lessonNum = row + 1;
        final WeekScheduleItem item = mData.get(lessonNum, day);
        StateListDrawable background = new StateListDrawable();
        if (item != null) {
            ((TextView)cell.findViewById(R.id.schedule_item_subject)).setText(item.label1);
            ((TextView)cell.findViewById(R.id.schedule_item_teacher)).setText(item.label2);

            background.addState(new int[]{android.R.attr.state_pressed},
                    getContext().getResources().getDrawable(R.drawable.mark_item_background));
            background.addState(new int[]{android.R.attr.stateNotNeeded},
                    new LayerDrawable(
                        new Drawable[]{
                            new ColorDrawable(item.color),
                            getContext().getResources().getDrawable(R.drawable.border_left_bottom)
                        }
                    ));
        } else {
            background.addState(new int[]{android.R.attr.state_pressed},
                    getContext().getResources().getDrawable(R.drawable.mark_item_background));
            background.addState(new int[]{android.R.attr.stateNotNeeded},
                    getContext().getResources().getDrawable(R.drawable.border_left_bottom));
        }
        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onItemClicked(item);
                }
            }
        });
    }

    public void setDataItemWidth(int width) {
        mDataItemWidth = width;
    }

    @Override
    public void swapCursor(Cursor c) {
        mDayColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule.FIELD_DAY);
        mLessonNumColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule.FIELD_LESSON_NUMBER);
        mLocalIdColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule._ID);
        mRemoteIdColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule.REMOTE_ID);
        mGroupColumnIndex = c.getColumnIndexOrThrow(Contract.Groups.FIELD_NAME);
        mTeacherColumnIndex = c.getColumnIndexOrThrow("teacher");
        mSubjectColumnIndex = c.getColumnIndexOrThrow(Contract.Subjects.FIELD_NAME);
        mColorColumnIndex = c.getColumnIndexOrThrow(Contract.Schedule.FIELD_COLOR);
        super.swapCursor(c);
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
        public void onItemClicked(WeekScheduleItem item);
    }

    /*private ScheduleItemClickListener mClickListener;
    private int mLeftBorderWidth;
    private int mRightBorderWidth;
    private int mItemWidth;

    public ScheduleRowAdapter(Context context, WeekSchedule schedule, String[] timeIntervals) {
        super(context, null, COLS_COUNT, null, null);
        setCellsLayouts();
        changeSchedule(schedule, timeIntervals, false);
    }

    public ScheduleRowAdapter(Context context, WeekSchedule schedule, String[] timeIntervals, int bordersWidth, int itemWidth) {
        super(context, null, COLS_COUNT, null, null);
        setCellsLayouts();
        changeSchedule(schedule, timeIntervals, false);

        int[] columnsWidths = new int[COLS_COUNT];
        columnsWidths[0] = bordersWidth;
        columnsWidths[COLS_COUNT - 1] = bordersWidth;
        for (int i = 1; i < COLS_COUNT - 1; i++) {
            columnsWidths[i] = itemWidth;
        }
        setColumnsWidths(columnsWidths, false);
    }

    public void setItemClickListener(ScheduleItemClickListener listener) {
        mClickListener = listener;
    }

    public void setSchedule(WeekSchedule schedule, String[] timeIntervals) {
        changeSchedule(schedule, timeIntervals, true);
    }

    public void setLeftHeaderWidth(int width) {
        mLeftBorderWidth = width;
        setColumnWidthAtPos(0, mLeftBorderWidth);
    }

    public void setItemWidth(int width) {
        mItemWidth = width;
        for (int i = 1; i < COLS_COUNT; i++) {
            setColumnWidthAtPos(i, mItemWidth);
        }
    }

    private void setCellsLayouts() {
        setCellLayoutAtPos(0, LEFT_BORDER_ITEM_LAYOUT, false);
        setCellLayoutAtPos(COLS_COUNT - 1, RIGHT_BORDER_ITEM_LAYOUT, false);
        for (int i = 1; i < COLS_COUNT - 1; i++) {
            setCellLayoutAtPos(i, ITEM_LAYOUT, false);
        }
    }

    private void changeSchedule(WeekSchedule schedule, String[] timeIntervals, boolean notifyChanged) {
        int rowsCount = schedule.getLastItemPositionInLongestDay() + 1;
        Object[] data = new Object[COLS_COUNT * rowsCount];
        for (int i = 0; i < rowsCount; i++) {
            for (int j = 0; j < COLS_COUNT; j++) {
                int index = i * COLS_COUNT + j;
                if (j == 0) {
                    data[index] = i + 1;
                } else if (j == COLS_COUNT - 1) {
                    String[] intervals = timeIntervals[i].split(" ");
                    data[index] = intervals;
                } else {
                    data[index] = schedule.getScheduleItem(j - 1, i);
                }
            }
        }
        setData(data, notifyChanged);
    }

    private boolean isBorderItemView(int column) {
        return isLeftItemView(column) || isRightItemView(column);
    }

    private boolean isRightItemView(int column) {
        return (column + 1) % COLS_COUNT == 0;
    }

    private boolean isLeftItemView(int column) {
        return column % COLS_COUNT == 0;
    }

    @Override
    public void defineNewViewHolder(int row, int column, View view) {
        if (isBorderItemView(column)) {
            if (isRightItemView(column)) {
                RightItemViewHolder holder = new RightItemViewHolder();
                holder.timeStartView = (TextView) view.findViewById(R.id.schedule_time_start);
                holder.timeEndView = (TextView) view.findViewById(R.id.schedule_time_end);
                view.setTag(holder);
            }
            return;
        }
        MainItemViewHolder holder = new MainItemViewHolder();
        holder.groupView = (TextView) view.findViewById(R.id.schedule_item_group);
        holder.subjectView = (TextView) view.findViewById(R.id.schedule_item_subject);
        view.setTag(holder);
    }

    @Override
    public void bindSingleViewToData(final int row, final int column, View view, Object dataObject) {
        if (isBorderItemView(column)) {
            if (isRightItemView(column)) {
                RightItemViewHolder holder = (RightItemViewHolder) view.getTag();
                String[] interval = (String[]) dataObject;
                holder.timeStartView.setText(interval[0]);
                holder.timeEndView.setText(interval[1]);
            } else {
                TextView positionView = (TextView) view;
                positionView.setText(dataObject.toString());
            }
            return;
        }
        Lesson lesson = (Lesson) dataObject;
        if (lesson != null) {
            MainItemViewHolder holder = (MainItemViewHolder) view.getTag();
            holder.groupView.setText(lesson.getGroup().label);
            holder.subjectView.setText(lesson.getSubject().label);

            final ColorDrawable color = new ColorDrawable(lesson.getColor());
            LayerDrawable background = new LayerDrawable(
                    new Drawable[]{
                            color,
                            getContext().getResources().getDrawable(ITEM_BACKGROUND_IDLE_BORDERS)
                    }
            );
            view.setBackground(background);
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    int action = motionEvent.getAction();
                    if (action == MotionEvent.ACTION_DOWN) {
                        LayerDrawable background = new LayerDrawable(
                                new Drawable[]{
                                        color,
                                        new ColorDrawable(getContext().getResources().getColor(R.color.lighten)),
                                        getContext().getResources().getDrawable(ITEM_BACKGROUND_IDLE_BORDERS)
                                }
                        );
                        view.setBackground(background);
                    } else if (action == MotionEvent.ACTION_UP
                            || action == MotionEvent.ACTION_POINTER_UP
                            || action == MotionEvent.ACTION_CANCEL) {
                        LayerDrawable background = new LayerDrawable(
                                new Drawable[]{
                                        color,
                                        getContext().getResources().getDrawable(ITEM_BACKGROUND_IDLE_BORDERS)
                                }
                        );
                        view.setBackground(background);
                    }
                    return false;
                }
            });
        } else {
            clearSingleView(view);
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mClickListener != null) {
                    mClickListener.onClick(column, row);
                }
            }
        });
    }

    private void clearSingleView(View view) {
        Object holder = view.getTag();
        if (holder != null) {
            if (holder instanceof MainItemViewHolder) {
                MainItemViewHolder mainHolder = (MainItemViewHolder) holder;
                mainHolder.groupView.setText("");
                mainHolder.subjectView.setText("");
                view.setBackgroundResource(ITEM_BACKGROUND_IDLE_CLICKABLE);
                view.setOnTouchListener(null);
            } else if (holder instanceof RightItemViewHolder) {
                RightItemViewHolder rightHolder = (RightItemViewHolder) holder;
                rightHolder.timeEndView.setText("");
                rightHolder.timeStartView.setText("");
            }
        }
    }

    public interface ScheduleItemClickListener {
        public void onClick(int day, int position);
    }*/
}
