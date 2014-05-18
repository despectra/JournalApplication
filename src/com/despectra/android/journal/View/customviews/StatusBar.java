package com.despectra.android.journal.view.customviews;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.despectra.android.journal.R;
import org.w3c.dom.Text;

/**
 * Created by Dmitry on 10.04.2014.
 */
public class StatusBar extends RelativeLayout {

    private ImageView mSpinner;
    private TextView mStatusText;
    private AnimatorSet mSpinnerAnimator;

    public StatusBar(Context context) {
        super(context);
        init(context);
    }

    public StatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatusBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.status_bar, this, true);
        mSpinner = (ImageView) findViewById(R.id.status_bar_spinner);
        mStatusText = (TextView) findViewById(R.id.status_bar_text);
        mSpinnerAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.small_spinner_rotate);
        mSpinnerAnimator.setTarget(mSpinner);
        mSpinnerAnimator.setInterpolator(new LinearInterpolator());
    }

    public void showSpinner() {
        mSpinner.setVisibility(View.VISIBLE);
        mSpinnerAnimator.start();
    }

    public void hideSpinner() {
        mSpinnerAnimator.end();
        mSpinner.setVisibility(View.GONE);
    }

    public void showStatus(String text) {
        mStatusText.setText(text);
        mStatusText.animate().alpha(1.0f).setDuration(200).start();
    }

    public void showStatusThenHide(String text, long showDurationMillis) {
        setStatusText(text);
        mStatusText.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideStatus();
            }
        }, showDurationMillis);
    }

    public void hideStatus() {
        mStatusText.animate().alpha(0.0f).setDuration(200).start();
    }

    public void setStatusText(String text) {
        mStatusText.setText(text);
    }

}
