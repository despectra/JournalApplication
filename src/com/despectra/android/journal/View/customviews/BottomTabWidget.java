package com.despectra.android.journal.view.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabWidget;
import android.widget.TextView;
import com.despectra.android.journal.R;

import java.util.List;

/**
 * Created by Dmirty on 05.03.14.
 */
public class BottomTabWidget extends TabWidget implements View.OnClickListener {
    private OnTabSelectedListener mTabListener;
    private View mCurrentTab;
    private int mCurrentTabIndex;

    public BottomTabWidget(Context context) {
        super(context);
        setBackgroundResource(R.drawable.bottom_tabs_bg);
    }

    public BottomTabWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.bottom_tabs_bg);
    }

    public BottomTabWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setBackgroundResource(R.drawable.bottom_tabs_bg);
    }

    public void setTabsList(List<String> tabs) {
        removeAllViews();
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        for (int i = 0; i < tabs.size(); i++) {
            View tab = LayoutInflater.from(getContext()).inflate(R.layout.bottom_tab_item, null, false);
            tab.setBackgroundResource(R.drawable.bottom_tab_item_background);
            tab.setTag(i);
            tab.setOnClickListener(this);
            TextView tabLabel = (TextView)tab.findViewById(R.id.bottom_tab_item);
            tabLabel.setText(tabs.get(i));
            addView(tab, params);
        }
        setCurrentTab(0);
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        mTabListener = listener;
    }

    @Override
    public void onClick(View view) {
        int index = (Integer)view.getTag();
        if (mCurrentTabIndex != index) {
            setCurrentTab(index);
            if(mTabListener != null) {
                mTabListener.onTabSelected(index);
            }
        }
    }

    @Override
    public void setCurrentTab(int index) {
        if (mCurrentTab != null) {
            mCurrentTab.setActivated(false);
        }
        mCurrentTabIndex = index;
        mCurrentTab = getChildTabViewAt(index);
        mCurrentTab.setActivated(true);
    }

    public int getCurrentTabIndex() {
        return mCurrentTabIndex;
    }

    public interface OnTabSelectedListener {
        public void onTabSelected(int index);
    }
}
