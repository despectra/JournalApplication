package com.despectra.android.journal.view;

import android.content.Context;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ListView;
import com.despectra.android.journal.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dmirty on 20.02.14.
 */
public class AbsListViewsDrawSynchronizer {
    private static final String TAG = "SYNCHRONIZER";
    private ListView mFirstView;
    private ListView mSecondView;
    private Map<ListView, ViewTreeObserver.OnDrawListener> mViewToListenerMap;
    private boolean mScrolling;
    private Callback mCallback;

    private ViewTreeObserver.OnDrawListener mFirstDrawListener = new ViewTreeObserver.OnDrawListener() {
        @Override
        public void onDraw() {
            mSecondView.postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    setListScrolling(mSecondView, mFirstView.getFirstVisiblePosition(), Utils.getAbsListViewOffset(mFirstView));
                }
            });
        }
    };

    private ViewTreeObserver.OnDrawListener mSecondDrawListener = new ViewTreeObserver.OnDrawListener() {
        @Override
        public void onDraw() {
            mFirstView.postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    setListScrolling(mFirstView, mSecondView.getFirstVisiblePosition(), Utils.getAbsListViewOffset(mSecondView));
                }
            });
        }
    };


    private AbsListView.OnScrollListener mScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView absListView, int state) {
            if (!mScrolling && state != SCROLL_STATE_IDLE) {
                absListView.getViewTreeObserver().addOnDrawListener(mViewToListenerMap.get(absListView));
                mScrolling = true;
            } else if (state == SCROLL_STATE_IDLE) {
                absListView.getViewTreeObserver().removeOnDrawListener(mViewToListenerMap.get(absListView));
                mScrolling = false;
                if(mCallback != null) {
                    mCallback.onScrollingStopped(absListView);
                }
            }
        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        }
    };


    public AbsListViewsDrawSynchronizer(Context c, ListView v1, ListView v2) {
        mFirstView = v1;
        mSecondView = v2;

        mViewToListenerMap = new HashMap<ListView, ViewTreeObserver.OnDrawListener>(2);
        updateMap();
        attachListeners();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setNewViews(ListView v1, ListView v2) {
        removeListeners();
        mFirstView = v1;
        mSecondView = v2;
        updateMap();
        attachListeners();
    }

    private void attachListeners() {
        mFirstView.setOnScrollListener(mScrollListener);
        mSecondView.setOnScrollListener(mScrollListener);
    }

    private void removeListeners() {
        mFirstView.setOnScrollListener(null);
        mSecondView.setOnScrollListener(null);
    }

    private void updateMap() {
        mViewToListenerMap.clear();
        mViewToListenerMap.put(mFirstView, mFirstDrawListener);
        mViewToListenerMap.put(mSecondView, mSecondDrawListener);
    }

    private void setListScrolling(final ListView view, final int position, final int offset) {
        view.setSelectionFromTop(position, offset);
    }


    public interface Callback {
        public void onScrollingStopped(AbsListView view);
    }
}
