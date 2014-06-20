package com.despectra.android.journal.view.schedule;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.despectra.android.journal.R;
import com.despectra.android.journal.logic.local.Contract;
import com.despectra.android.journal.logic.model.WeekSchedule;
import com.despectra.android.journal.model.EntityIdsColumns;
import com.despectra.android.journal.utils.Utils;

/**
 * Created by Dmitry on 13.03.14.
 */
public class WeekScheduleView extends LinearLayout {

    public static final String TAG = "VIEW_Schedule";

    public static final String[] DAYS = {"Пн.", "Вт.", "Ср.", "Чт.", "Пт.", "Сб.", "Вс."};

    private OnEventSelectedListener mEventSelectedListener;
    private GridView mHeaderView;
    private ListView mDataView;
    private ScheduleRowAdapter mScheduleAdapter;
    private String[] mTimeIntervals;
    private View mItemView;
    private int mHeaderHeight;
    private int mItemWidth;
    private Cursor mCursor;
    private ArrayAdapter<String> mHeaderAdapter;

    public WeekScheduleView(Context context) {
        super(context);
        init(context);
    }

    public WeekScheduleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WeekScheduleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setOnEventSelectedListener(OnEventSelectedListener listener) {
        mEventSelectedListener = listener;
    }

    public void updateSchedule(Cursor cursor) {
        mScheduleAdapter.swapCursor(cursor);
    }

    /*public void setSchedule(WeekSchedule schedule, String[] timeIntervals) {
        mSchedule = schedule;
        mTimeIntervals = timeIntervals;
        if (mScheduleAdapter != null) {
            mScheduleAdapter.setSchedule(mSchedule, mTimeIntervals);
        } else {
            mScheduleAdapter = new ScheduleRowAdapter(getContext(), mSchedule, mTimeIntervals);
            mScheduleAdapter.setItemClickListener(new ScheduleRowAdapter.ScheduleItemClickListener() {
                @Override
                public void onClick(int day, int position) {
                    if (mEventSelectedListener != null) {
                        mEventSelectedListener.onEventSelected(day, position);
                    }
                }
            });
            mDataView.setAdapter(mScheduleAdapter);
        }
    }*/

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.schedule_view, this, true);
        mHeaderView = (GridView) findViewById(R.id.schedule_top_header);
        mDataView = (ListView) findViewById(R.id.schedule_container);
        mScheduleAdapter = new ScheduleRowAdapter(getContext(), 10);
        mDataView.setAdapter(mScheduleAdapter);
        initHeader(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mScheduleAdapter.recalculateColumnsWidths(getMeasuredWidth());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mHeaderHeight = 50;
       /* mItemWidth = (w - 2 * mHeaderHeight) / 7;
        mScheduleAdapter.setLeftColumnWidth(mHeaderHeight);
        mScheduleAdapter.setRightColumnWidth(mHeaderHeight);
        mScheduleAdapter.setDataItemWidth(mItemWidth);
        mHeaderView.setColumnWidth(mItemWidth);*/

        LinearLayout.LayoutParams headerViewParams = (LayoutParams) mHeaderView.getLayoutParams();
        headerViewParams.width = w - 2 * mHeaderHeight + 1;
        headerViewParams.height = mHeaderHeight;
        headerViewParams.rightMargin = mHeaderHeight - 1;
        mHeaderView.setAdapter(mHeaderAdapter);
    }

    private void initHeader(Context context) {
        mHeaderView.setEnabled(false);
        mHeaderAdapter = new ArrayAdapter<String>(context, R.layout.small_header_item, DAYS) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                Utils.setViewHeight(v, mHeaderHeight);
                return v;
            }
        };
    }

    public interface OnEventSelectedListener {
        public void onEventSelected(int day, int position);
    }
}
