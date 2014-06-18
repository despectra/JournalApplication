package com.despectra.android.journal.view.schedule;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.despectra.android.journal.logic.model.WeekSchedule;

/**
 * Created by Dmitry on 13.03.14.
 */
public class WeekScheduleView extends LinearLayout {

    public static final String TAG = "VIEW_Schedule";

    public static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private OnEventSelectedListener mEventSelectedListener;
    private GridView mHeaderView;
    private ListView mDataView;
    private ScheduleRowAdapter mScheduleAdapter;
    private WeekSchedule mSchedule;
    private String[] mTimeIntervals;
    private View mItemView;
    private int mHeaderHeight;
    private int mItemWidth;

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

    public void setSchedule(WeekSchedule schedule, String[] timeIntervals) {
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
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.schedule_view, this, true);
        mHeaderView = (GridView) findViewById(R.id.schedule_top_header);
        mDataView = (ListView) findViewById(R.id.schedule_container);

        initHeader(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mHeaderHeight = (w+h) / 38;
        mItemWidth = (w - 2 * mHeaderHeight) / 7;
        mScheduleAdapter.setLeftHeaderWidth(mHeaderHeight);
        mScheduleAdapter.setItemWidth(mItemWidth);
        mHeaderView.setColumnWidth(mItemWidth);

        LinearLayout.LayoutParams headerViewParams = (LayoutParams) mHeaderView.getLayoutParams();
        headerViewParams.width = w - 2 * mHeaderHeight;
        headerViewParams.height = mHeaderHeight;
        headerViewParams.weight = 0;
        headerViewParams.rightMargin = mHeaderHeight;
        mHeaderView.setLayoutParams(headerViewParams);
    }

    private void initHeader(Context context) {
        mHeaderView.setEnabled(false);
        ArrayAdapter<String> headerAdapter = new ArrayAdapter<String>(context, R.layout.small_header_item, DAYS);
        mHeaderView.setAdapter(headerAdapter);
    }

    public interface OnEventSelectedListener {
        public void onEventSelected(int day, int position);
    }
}
