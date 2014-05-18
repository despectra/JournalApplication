package com.despectra.android.journal.view.schedule;

/**
 * Created by Dmirty on 17.02.14.
 */
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.despectra.android.journal.R;

public class CurrentScheduleAdapter extends BaseAdapter {

    public static final int STATUS_PAST = 0;
    public static final int STATUS_CURRENT = 1;
    public static final int STATUS_FUTURE = 2;
    public static final String[] STATUSES = {"урок окончен", "идет урок", "далее"};

    private Model[] mData;
    private Context mContext;

    public CurrentScheduleAdapter(Context context, Model[] data) {
        mData = data;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mData.length;
    }

    @Override
    public Object getItem(int position) {
        return mData[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = LayoutInflater.from(mContext).inflate(R.layout.current_schedule_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.subjectColorIcon = v.findViewById(R.id.subject_icon_view);
            holder.subjectTextView = (TextView) v.findViewById(R.id.subject_view);
            holder.groupColorIcon = v.findViewById(R.id.group_icon_view);
            holder.groupTextView = (TextView) v.findViewById(R.id.group_view);
            holder.timeTextView = (TextView) v.findViewById(R.id.time_view);
            holder.statusTextView = (TextView) v.findViewById(R.id.status_view);
            v.setTag(holder);
        } else {
            v = convertView;
        }
        inflateData(position, v);
        return v;
    }

    private void inflateData(int position, View v) {
        Model m = mData[position];
        int status = m.status;
        switch(status) {
            case STATUS_PAST:
                v.setBackgroundResource(R.color.lesson_completed_background);
                break;
            case STATUS_CURRENT:
                v.setBackgroundResource(R.drawable.lesson_current_drawable);
                break;
            case STATUS_FUTURE:
                v.setBackgroundResource(R.drawable.lesson_future_drawable);
                break;
        }
        ViewHolder holder = (ViewHolder) v.getTag();
        holder.subjectTextView.setText(m.subject);
        holder.groupTextView.setText(m.group);
        holder.timeTextView.setText(String.format("%s - %s", m.timeStart, m.timeEnd));
        holder.statusTextView.setText(STATUSES[status]);
        holder.groupColorIcon.setBackgroundColor(m.groupColor);
        holder.subjectColorIcon.setBackgroundColor(m.subjectColor);
    }

    private static class ViewHolder {
        public TextView statusTextView;
        public TextView groupTextView;
        public TextView subjectTextView;
        public TextView timeTextView;
        public View groupColorIcon;
        public View subjectColorIcon;
    }

    public static class Model {
        public Model(int status, String group, String subject, String timeStart, String timeEnd, int groupColor, int subjectColor) {
            this.status = status;
            this.group = group;
            this.subject = subject;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
            this.groupColor = groupColor;
            this.subjectColor = subjectColor;
        }

        public int status;
        public String group;
        public String subject;
        public String timeStart;
        public String timeEnd;
        public int groupColor;
        public int subjectColor;
    }

}
