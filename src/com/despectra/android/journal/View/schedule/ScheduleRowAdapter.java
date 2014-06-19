package com.despectra.android.journal.view.schedule;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.view.PseudoGridAdapter;

/**
 * Created by Dmitry on 14.03.14.
 */
public class ScheduleRowAdapter extends PseudoGridAdapter {
    private static final String TAG = "ScheduleRowAdapter";
    private static final int COLS_COUNT = 9;
    public static final int COL_NUM = 0;
    public static final int COL_TIME = 8;
    public static final int LESSON_NUM_ITEM_LAYOUT = R.layout.week_schedule_position_item;
    public static final int TIME_INTERVAL_ITEM_LAYOUT = R.layout.week_schedule_time_item;
    private static final int ITEM_LAYOUT = R.layout.week_schedule_item;
    public static final int ITEM_BACKGROUND_IDLE_BORDERS = R.drawable.border_left_top;
    public static final int ITEM_BACKGROUND_IDLE_CLICKABLE = R.drawable.schedule_item_background_selector;

    public ScheduleRowAdapter(Context context, Cursor c, EntityIdsColumns[] idsColumns, String entityStatusColumn,
                              int cellLayout, int columnsCount, int dataColsCount, int[] colsWidths, String[] from, int[] to) {
        super(context,
                c,
                idsColumns,
                entityStatusColumn,
                cellLayout,
                columnsCount,
                dataColsCount,
                colsWidths,
                from,
                to);
    }

    @Override
    protected boolean isDataColumn(int column) {
        return column >= 1 && column <= 7;
    }

    @Override
    protected View newNonDataColumn(int column) {
        switch (column) {
            case COL_NUM:
                return View.inflate(getContext(), LESSON_NUM_ITEM_LAYOUT, null);
            case COL_TIME:
                return View.inflate(getContext(), TIME_INTERVAL_ITEM_LAYOUT, null);
            default:
                return null;
        }
    }

    @Override
    protected void bindNonDataColumn(int row, int column, View cell, Cursor cursor) {
        switch (column) {
            case COL_NUM:
                TextView numView = (TextView) cell.findViewById(R.id.text1);
                numView.setText(row + 1);
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
    protected void bindDataColumn(int row, View cell, Cursor cursor) {
        super.bindView(cell, getContext(), cursor);

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
