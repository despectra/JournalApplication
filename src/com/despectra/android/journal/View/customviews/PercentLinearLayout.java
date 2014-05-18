package com.despectra.android.journal.view.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.despectra.android.journal.R;
import com.despectra.android.journal.Utils;

/**
 * Created by Dmirty on 06.03.14.
 */
public class PercentLinearLayout extends ViewGroup {

    public static final int DIRECT = 0;
    public static final int INVERSE = 1;
    private static final String TAG = "PERCENT LAYOUT";

    private int mScreenWidth;
    private int mScreenHeight;
    private int mDirection = 0;
    private Rect mDrawingRect;
    private int mPercentSum = 0;

    public PercentLinearLayout(Context context) {
        super(context);
        init(null);
    }

    public PercentLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PercentLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public int getDirection() {
        return mDirection;
    }

    private void init(AttributeSet attrs) {
        setWillNotDraw(false);
        Point screenDimension = Utils.getDisplayDimension(getContext());
        mScreenWidth = screenDimension.x;
        mScreenHeight = screenDimension.y;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PercentLinearLayout);
            mDirection = a.getInteger(R.styleable.PercentLinearLayout_direction, 0);
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec),
                measuredHeight = MeasureSpec.getSize(heightMeasureSpec),
                widthMode = MeasureSpec.getMode(widthMeasureSpec),
                heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int childCount = getChildCount();
        int maxChildHeight = 0;
        mPercentSum = 0;
        View child;
        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);
            LayoutParams childParams = (LayoutParams) child.getLayoutParams();
            int widthPercent = childParams.widthPercent;
            int childWidth = (int)(measuredWidth * widthPercent / 100.0f);
            int childHeight = measuredHeight;
            child.measure(
                    MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
            );
            if (heightMode == MeasureSpec.AT_MOST) {
                maxChildHeight = Math.max(maxChildHeight, childHeight);
            }
            mPercentSum += widthPercent;
        }
        measuredWidth = (int)(measuredWidth * mPercentSum / 100.0f);
        measuredHeight = (heightMode == MeasureSpec.AT_MOST) ? maxChildHeight : measuredHeight;
        if (mDirection == DIRECT) {
            mDrawingRect = new Rect(0, 0, measuredWidth, measuredHeight);
        } else {
            mDrawingRect = new Rect(mScreenWidth - measuredWidth, 0, mScreenWidth, measuredHeight);
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childCount = getChildCount();
        int lastX = (mDirection == DIRECT) ? left : mScreenWidth;
        View child;
        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);
            LayoutParams childParams = (LayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            if (mDirection == DIRECT) {
                child.layout(lastX, childParams.topMargin, lastX + childWidth, childHeight);
                lastX += childWidth;
            } else {
                child.layout(lastX - childWidth, childParams.topMargin, lastX, childHeight);
                lastX -= childWidth;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.clipRect(mDrawingRect, Region.Op.REPLACE);
        super.onDraw(canvas);
    }

    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new PercentLinearLayout.LayoutParams(getContext(), attrs);
    }

    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof PercentLinearLayout.LayoutParams;
    }

    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new PercentLinearLayout.LayoutParams(p);
    }

    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    public void smoothScrollByPercent(int percent, int duration, Runnable startAction, Runnable endAction) {
        float scrollBy = mScreenWidth * percent / 100.0f;
        animate()
                .translationXBy(scrollBy)
                .setDuration(duration)
                .withStartAction(startAction)
                .withEndAction(endAction)
                .start();
    }

    public void setTranslationXByPercent(int percent) {
        setTranslationX(mScreenWidth * percent / 100.0f);
    }


    public static class LayoutParams extends MarginLayoutParams {
        int widthPercent = 100;
        int heightPercent = 100;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.PercentLinearLayout);
            widthPercent = a.getInteger(R.styleable.PercentLinearLayout_layout_width_percent, 100);
            heightPercent = a.getDimensionPixelSize(R.styleable.PercentLinearLayout_layout_height_percent, 100);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            if (source instanceof LayoutParams) {
                LayoutParams params = (LayoutParams) source;
                widthPercent = params.widthPercent;
                heightPercent = params.heightPercent;
            }
        }

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

    }
}
