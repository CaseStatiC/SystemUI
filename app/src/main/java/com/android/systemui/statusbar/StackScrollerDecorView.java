/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;

import com.android.systemui.statusbar.phone.PhoneStatusBar;

/**
 * A common base class for all views in the notification stack scroller which don't have a
 * background.
 */
public abstract class StackScrollerDecorView extends ExpandableView {
    public static final String TAG = "StackScrollerDecorView";
    protected View mContent;
    private boolean mIsVisible;
    private boolean mAnimating;

    public StackScrollerDecorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, "onFinishInflate: ");
        mContent = findContentView();
        setInvisible();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.d(TAG, "onLayout: ");
        setOutlineProvider(null);
    }

    @Override
    public boolean isTransparent() {
        Log.d(TAG, "isTransparent: ");
        return true;
    }

    public void performVisibilityAnimation(boolean nowVisible) {
        Log.d(TAG, "performVisibilityAnimation: ");
        animateText(nowVisible, null /* onFinishedRunnable */);
    }

    public void performVisibilityAnimation(boolean nowVisible, Runnable onFinishedRunnable) {
        Log.d(TAG, "performVisibilityAnimation: ");
        animateText(nowVisible, onFinishedRunnable);
    }

    public boolean isVisible() {
        Log.d(TAG, "isVisible: ");
        return mIsVisible || mAnimating;
    }

    /**
     * Animate the text to a new visibility.
     *
     * @param nowVisible should it now be visible
     * @param onFinishedRunnable A runnable which should be run when the animation is
     *        finished.
     */
    private void animateText(boolean nowVisible, final Runnable onFinishedRunnable) {
        Log.d(TAG, "animateText: ");
        if (nowVisible != mIsVisible) {
            // Animate text
            float endValue = nowVisible ? 1.0f : 0.0f;
            Interpolator interpolator;
            if (nowVisible) {
                interpolator = PhoneStatusBar.ALPHA_IN;
            } else {
                interpolator = PhoneStatusBar.ALPHA_OUT;
            }
            mAnimating = true;
            mContent.animate()
                    .alpha(endValue)
                    .setInterpolator(interpolator)
                    .setDuration(260)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mAnimating = false;
                            if (onFinishedRunnable != null) {
                                onFinishedRunnable.run();
                            }
                        }
                    });
            mIsVisible = nowVisible;
        } else {
            if (onFinishedRunnable != null) {
                onFinishedRunnable.run();
            }
        }
    }

    public void setInvisible() {
        Log.d(TAG, "setInvisible: ");
        mContent.setAlpha(0.0f);
        mIsVisible = false;
    }

    @Override
    public void performRemoveAnimation(long duration, float translationDirection,
            Runnable onFinishedRunnable) {
        Log.d(TAG, "performRemoveAnimation: ");
        // TODO: Use duration
        performVisibilityAnimation(false);
    }

    @Override
    public void performAddAnimation(long delay, long duration) {
        Log.d(TAG, "performAddAnimation: ");
        // TODO: use delay and duration
        performVisibilityAnimation(true);
    }

    @Override
    public boolean hasOverlappingRendering() {
        Log.d(TAG, "hasOverlappingRendering: ");
        return false;
    }

    public void cancelAnimation() {
        Log.d(TAG, "cancelAnimation: ");
        mContent.animate().cancel();
    }

    protected abstract View findContentView();
}
