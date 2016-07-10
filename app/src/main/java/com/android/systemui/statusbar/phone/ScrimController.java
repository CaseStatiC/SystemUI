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

package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import com.android.systemui.R;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.stack.StackStateAnimator;

/**
 * Controls both the scrim behind the notifications and in front of the notifications (when a
 * security method gets shown).
 */
public class ScrimController implements ViewTreeObserver.OnPreDrawListener,
        HeadsUpManager.OnHeadsUpChangedListener {
    public static final String TAG = "ScrimController";
    public static final long ANIMATION_DURATION = 220;
    public static final Interpolator KEYGUARD_FADE_OUT_INTERPOLATOR
            = new PathInterpolator(0f, 0, 0.7f, 1f);

    private static final float SCRIM_BEHIND_ALPHA = 0.62f;
    private static final float SCRIM_BEHIND_ALPHA_KEYGUARD = 0.45f;
    private static final float SCRIM_BEHIND_ALPHA_UNLOCKING = 0.2f;
    private static final float SCRIM_IN_FRONT_ALPHA = 0.75f;
    private static final int TAG_KEY_ANIM = R.id.scrim;
    private static final int TAG_KEY_ANIM_TARGET = R.id.scrim_target;
    private static final int TAG_HUN_START_ALPHA = R.id.hun_scrim_alpha_start;
    private static final int TAG_HUN_END_ALPHA = R.id.hun_scrim_alpha_end;

    private final ScrimView mScrimBehind;
    private final ScrimView mScrimInFront;
    private final UnlockMethodCache mUnlockMethodCache;
    private final View mHeadsUpScrim;

    private boolean mKeyguardShowing;
    private float mFraction;

    private boolean mDarkenWhileDragging;
    private boolean mBouncerShowing;
    private boolean mWakeAndUnlocking;
    private boolean mAnimateChange;
    private boolean mUpdatePending;
    private boolean mExpanding;
    private boolean mAnimateKeyguardFadingOut;
    private long mDurationOverride = -1;
    private long mAnimationDelay;
    private Runnable mOnAnimationFinished;
    private final Interpolator mInterpolator = new DecelerateInterpolator();
    private BackDropView mBackDropView;
    private boolean mScrimSrcEnabled;
    private boolean mDozing;
    private float mDozeInFrontAlpha;
    private float mDozeBehindAlpha;
    private float mCurrentInFrontAlpha;
    private float mCurrentBehindAlpha;
    private float mCurrentHeadsUpAlpha = 1;
    private int mPinnedHeadsUpCount;
    private float mTopHeadsUpDragAmount;
    private View mDraggedHeadsUpView;
    private boolean mForceHideScrims;
    private boolean mSkipFirstFrame;
    private boolean mDontAnimateBouncerChanges;

    public ScrimController(ScrimView scrimBehind, ScrimView scrimInFront, View headsUpScrim,
            boolean scrimSrcEnabled) {
        mScrimBehind = scrimBehind;
        mScrimInFront = scrimInFront;
        mHeadsUpScrim = headsUpScrim;
        final Context context = scrimBehind.getContext();
        mUnlockMethodCache = UnlockMethodCache.getInstance(context);
        mScrimSrcEnabled = scrimSrcEnabled;
        updateHeadsUpScrim(false);
    }

    public void setKeyguardShowing(boolean showing) {
        Log.d(TAG, "setKeyguardShowing: ");
        mKeyguardShowing = showing;
        scheduleUpdate();
    }

    public void onTrackingStarted() {
        Log.d(TAG, "onTrackingStarted: ");
        mExpanding = true;
        mDarkenWhileDragging = !mUnlockMethodCache.canSkipBouncer();
    }

    public void onExpandingFinished() {
        Log.d(TAG, "onExpandingFinished: ");
        mExpanding = false;
    }

    public void setPanelExpansion(float fraction) {
        Log.d(TAG, "setPanelExpansion: ");
        if (mFraction != fraction) {
            mFraction = fraction;
            scheduleUpdate();
            if (mPinnedHeadsUpCount != 0) {
                updateHeadsUpScrim(false);
            }
        }
    }

    public void setBouncerShowing(boolean showing) {
        Log.d(TAG, "setBouncerShowing: ");
        mBouncerShowing = showing;
        mAnimateChange = !mExpanding && !mDontAnimateBouncerChanges;
        scheduleUpdate();
    }

    public void setWakeAndUnlocking() {
        Log.d(TAG, "setWakeAndUnlocking: ");
        mWakeAndUnlocking = true;
        scheduleUpdate();
    }

    public void animateKeyguardFadingOut(long delay, long duration, Runnable onAnimationFinished,
            boolean skipFirstFrame) {
        Log.d(TAG, "animateKeyguardFadingOut: ");
        mWakeAndUnlocking = false;
        mAnimateKeyguardFadingOut = true;
        mDurationOverride = duration;
        mAnimationDelay = delay;
        mAnimateChange = true;
        mSkipFirstFrame = skipFirstFrame;
        mOnAnimationFinished = onAnimationFinished;
        scheduleUpdate();

        // No need to wait for the next frame to be drawn for this case - onPreDraw will execute
        // the changes we just scheduled.
        onPreDraw();
    }

    public void abortKeyguardFadingOut() {
        Log.d(TAG, "abortKeyguardFadingOut: ");
        if (mAnimateKeyguardFadingOut) {
            endAnimateKeyguardFadingOut(true /* force */);
        }
    }

    public void animateGoingToFullShade(long delay, long duration) {
        Log.d(TAG, "animateGoingToFullShade: ");
        mDurationOverride = duration;
        mAnimationDelay = delay;
        mAnimateChange = true;
        scheduleUpdate();
    }

    public void setDozing(boolean dozing) {
        Log.d(TAG, "setDozing: ");
        if (mDozing != dozing) {
            mDozing = dozing;
            scheduleUpdate();
        }
    }

    public void setDozeInFrontAlpha(float alpha) {
        Log.d(TAG, "setDozeInFrontAlpha: ");
        mDozeInFrontAlpha = alpha;
        updateScrimColor(mScrimInFront);
    }

    public void setDozeBehindAlpha(float alpha) {
        Log.d(TAG, "setDozeBehindAlpha: ");
        mDozeBehindAlpha = alpha;
        updateScrimColor(mScrimBehind);
    }

    public float getDozeBehindAlpha() {
        Log.d(TAG, "getDozeBehindAlpha: ");
        return mDozeBehindAlpha;
    }

    public float getDozeInFrontAlpha() {
        Log.d(TAG, "getDozeInFrontAlpha: ");
        return mDozeInFrontAlpha;
    }

    private void scheduleUpdate() {
        Log.d(TAG, "scheduleUpdate: ");
        if (mUpdatePending) return;

        // Make sure that a frame gets scheduled.
        mScrimBehind.invalidate();
        mScrimBehind.getViewTreeObserver().addOnPreDrawListener(this);
        mUpdatePending = true;
    }

    private void updateScrims() {
        Log.d(TAG, "updateScrims: ");
        if (mAnimateKeyguardFadingOut || mForceHideScrims) {
            setScrimInFrontColor(0f);
            setScrimBehindColor(0f);
        } else if (mWakeAndUnlocking) {

            // During wake and unlock, we first hide everything behind a black scrim, which then
            // gets faded out from animateKeyguardFadingOut.
            if (mDozing) {
                setScrimInFrontColor(0f);
                setScrimBehindColor(1f);
            } else {
                setScrimInFrontColor(1f);
                setScrimBehindColor(0f);
            }
        } else if (!mKeyguardShowing && !mBouncerShowing) {
            updateScrimNormal();
            setScrimInFrontColor(0);
        } else {
            updateScrimKeyguard();
        }
        mAnimateChange = false;
    }

    private void updateScrimKeyguard() {
        Log.d(TAG, "updateScrimKeyguard: ");
        if (mExpanding && mDarkenWhileDragging) {
            float behindFraction = Math.max(0, Math.min(mFraction, 1));
            float fraction = 1 - behindFraction;
            fraction = (float) Math.pow(fraction, 0.8f);
            behindFraction = (float) Math.pow(behindFraction, 0.8f);
            setScrimInFrontColor(fraction * SCRIM_IN_FRONT_ALPHA);
            setScrimBehindColor(behindFraction * SCRIM_BEHIND_ALPHA_KEYGUARD);
        } else if (mBouncerShowing) {
            setScrimInFrontColor(SCRIM_IN_FRONT_ALPHA);
            setScrimBehindColor(0f);
        } else {
            float fraction = Math.max(0, Math.min(mFraction, 1));
            setScrimInFrontColor(0f);
            setScrimBehindColor(fraction
                    * (SCRIM_BEHIND_ALPHA_KEYGUARD - SCRIM_BEHIND_ALPHA_UNLOCKING)
                    + SCRIM_BEHIND_ALPHA_UNLOCKING);
        }
    }

    private void updateScrimNormal() {
        Log.d(TAG, "updateScrimNormal: ");
        float frac = mFraction;
        // let's start this 20% of the way down the screen
        frac = frac * 1.2f - 0.2f;
        if (frac <= 0) {
            setScrimBehindColor(0);
        } else {
            // woo, special effects
            final float k = (float)(1f-0.5f*(1f-Math.cos(3.14159f * Math.pow(1f-frac, 2f))));
            setScrimBehindColor(k * SCRIM_BEHIND_ALPHA);
        }
    }

    private void setScrimBehindColor(float alpha) {
        Log.d(TAG, "setScrimBehindColor: ");
        setScrimColor(mScrimBehind, alpha);
    }

    private void setScrimInFrontColor(float alpha) {
        Log.d(TAG, "setScrimInFrontColor: ");
        setScrimColor(mScrimInFront, alpha);
        if (alpha == 0f) {
            mScrimInFront.setClickable(false);
        } else {

            // Eat touch events (unless dozing).
            mScrimInFront.setClickable(!mDozing);
        }
    }

    private void setScrimColor(View scrim, float alpha) {
        Log.d(TAG, "setScrimColor: ");
        ValueAnimator runningAnim = (ValueAnimator) scrim.getTag(TAG_KEY_ANIM);
        Float target = (Float) scrim.getTag(TAG_KEY_ANIM_TARGET);
        if (runningAnim != null && target != null) {
            if (alpha != target) {
                runningAnim.cancel();
            } else {
                return;
            }
        }
        if (mAnimateChange) {
            startScrimAnimation(scrim, alpha);
        } else {
            setCurrentScrimAlpha(scrim, alpha);
            updateScrimColor(scrim);
        }
    }

    private float getDozeAlpha(View scrim) {
        Log.d(TAG, "getDozeAlpha: ");
        return scrim == mScrimBehind ? mDozeBehindAlpha : mDozeInFrontAlpha;
    }

    private float getCurrentScrimAlpha(View scrim) {
        Log.d(TAG, "getCurrentScrimAlpha: ");
        return scrim == mScrimBehind ? mCurrentBehindAlpha
                : scrim == mScrimInFront ? mCurrentInFrontAlpha
                : mCurrentHeadsUpAlpha;
    }

    private void setCurrentScrimAlpha(View scrim, float alpha) {
        Log.d(TAG, "setCurrentScrimAlpha: ");
        if (scrim == mScrimBehind) {
            mCurrentBehindAlpha = alpha;
        } else if (scrim == mScrimInFront) {
            mCurrentInFrontAlpha = alpha;
        } else {
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            mCurrentHeadsUpAlpha = alpha;
        }
    }

    private void updateScrimColor(View scrim) {
        Log.d(TAG, "updateScrimColor: ");
        float alpha1 = getCurrentScrimAlpha(scrim);
        if (scrim instanceof ScrimView) {
            float alpha2 = getDozeAlpha(scrim);
            float alpha = 1 - (1 - alpha1) * (1 - alpha2);
            ((ScrimView) scrim).setScrimColor(Color.argb((int) (alpha * 255), 0, 0, 0));
        } else {
            scrim.setAlpha(alpha1);
        }
    }

    private void startScrimAnimation(final View scrim, float target) {
        Log.d(TAG, "startScrimAnimation: ");
        float current = getCurrentScrimAlpha(scrim);
        ValueAnimator anim = ValueAnimator.ofFloat(current, target);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = (float) animation.getAnimatedValue();
                setCurrentScrimAlpha(scrim, alpha);
                updateScrimColor(scrim);
            }
        });
        anim.setInterpolator(getInterpolator());
        anim.setStartDelay(mAnimationDelay);
        anim.setDuration(mDurationOverride != -1 ? mDurationOverride : ANIMATION_DURATION);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mOnAnimationFinished != null) {
                    mOnAnimationFinished.run();
                    mOnAnimationFinished = null;
                }
                scrim.setTag(TAG_KEY_ANIM, null);
                scrim.setTag(TAG_KEY_ANIM_TARGET, null);
            }
        });
        anim.start();
        if (mSkipFirstFrame) {
            anim.setCurrentPlayTime(16);
        }
        scrim.setTag(TAG_KEY_ANIM, anim);
        scrim.setTag(TAG_KEY_ANIM_TARGET, target);
    }

    private Interpolator getInterpolator() {
        Log.d(TAG, "getInterpolator: ");
        return mAnimateKeyguardFadingOut ? KEYGUARD_FADE_OUT_INTERPOLATOR : mInterpolator;
    }

    @Override
    public boolean onPreDraw() {
        Log.d(TAG, "onPreDraw: ");
        mScrimBehind.getViewTreeObserver().removeOnPreDrawListener(this);
        mUpdatePending = false;
        if (mDontAnimateBouncerChanges) {
            mDontAnimateBouncerChanges = false;
        }
        updateScrims();
        mDurationOverride = -1;
        mAnimationDelay = 0;
        mSkipFirstFrame = false;

        // Make sure that we always call the listener even if we didn't start an animation.
        endAnimateKeyguardFadingOut(false /* force */);
        return true;
    }

    private void endAnimateKeyguardFadingOut(boolean force) {
        Log.d(TAG, "endAnimateKeyguardFadingOut: ");
        mAnimateKeyguardFadingOut = false;
        if ((force || (!isAnimating(mScrimInFront) && !isAnimating(mScrimBehind)))
                && mOnAnimationFinished != null) {
            mOnAnimationFinished.run();
            mOnAnimationFinished = null;
        }
    }

    private boolean isAnimating(View scrim) {
        Log.d(TAG, "isAnimating: ");
        return scrim.getTag(TAG_KEY_ANIM) != null;
    }

    public void setBackDropView(BackDropView backDropView) {
        Log.d(TAG, "setBackDropView: ");
        mBackDropView = backDropView;
        mBackDropView.setOnVisibilityChangedRunnable(new Runnable() {
            @Override
            public void run() {
                updateScrimBehindDrawingMode();
            }
        });
        updateScrimBehindDrawingMode();
    }

    private void updateScrimBehindDrawingMode() {
        Log.d(TAG, "updateScrimBehindDrawingMode: ");
        boolean asSrc = mBackDropView.getVisibility() != View.VISIBLE && mScrimSrcEnabled;
        mScrimBehind.setDrawAsSrc(asSrc);
    }

    @Override
    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
        Log.d(TAG, "onHeadsUpPinnedModeChanged: ");
    }

    @Override
    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        Log.d(TAG, "onHeadsUpPinned: ");
        mPinnedHeadsUpCount++;
        updateHeadsUpScrim(true);
    }

    @Override
    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
        Log.d(TAG, "onHeadsUpUnPinned: ");
        mPinnedHeadsUpCount--;
        if (headsUp == mDraggedHeadsUpView) {
            mDraggedHeadsUpView = null;
            mTopHeadsUpDragAmount = 0.0f;
        }
        updateHeadsUpScrim(true);
    }

    @Override
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        Log.d(TAG, "onHeadsUpStateChanged: ");
    }

    private void updateHeadsUpScrim(boolean animate) {
        Log.d(TAG, "updateHeadsUpScrim: ");
        float alpha = calculateHeadsUpAlpha();
        ValueAnimator previousAnimator = StackStateAnimator.getChildTag(mHeadsUpScrim,
                TAG_KEY_ANIM);
        float animEndValue = -1;
        if (previousAnimator != null) {
            if (animate || alpha == mCurrentHeadsUpAlpha) {
                previousAnimator.cancel();
            } else {
                animEndValue = StackStateAnimator.getChildTag(mHeadsUpScrim, TAG_HUN_END_ALPHA);
            }
        }
        if (alpha != mCurrentHeadsUpAlpha && alpha != animEndValue) {
            if (animate) {
                startScrimAnimation(mHeadsUpScrim, alpha);
                mHeadsUpScrim.setTag(TAG_HUN_START_ALPHA, mCurrentHeadsUpAlpha);
                mHeadsUpScrim.setTag(TAG_HUN_END_ALPHA, alpha);
            } else {
                if (previousAnimator != null) {
                    float previousStartValue = StackStateAnimator.getChildTag(mHeadsUpScrim,
                            TAG_HUN_START_ALPHA);
                    float previousEndValue = StackStateAnimator.getChildTag(mHeadsUpScrim,
                           TAG_HUN_END_ALPHA);
                    // we need to increase all animation keyframes of the previous animator by the
                    // relative change to the end value
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    float relativeDiff = alpha - previousEndValue;
                    float newStartValue = previousStartValue + relativeDiff;
                    values[0].setFloatValues(newStartValue, alpha);
                    mHeadsUpScrim.setTag(TAG_HUN_START_ALPHA, newStartValue);
                    mHeadsUpScrim.setTag(TAG_HUN_END_ALPHA, alpha);
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                } else {
                    // update the alpha directly
                    setCurrentScrimAlpha(mHeadsUpScrim, alpha);
                    updateScrimColor(mHeadsUpScrim);
                }
            }
        }
    }

    /**
     * Set the amount the current top heads up view is dragged. The range is from 0 to 1 and 0 means
     * the heads up is in its resting space and 1 means it's fully dragged out.
     *
     * @param draggedHeadsUpView the dragged view
     * @param topHeadsUpDragAmount how far is it dragged
     */
    public void setTopHeadsUpDragAmount(View draggedHeadsUpView, float topHeadsUpDragAmount) {
        Log.d(TAG, "setTopHeadsUpDragAmount: ");
        mTopHeadsUpDragAmount = topHeadsUpDragAmount;
        mDraggedHeadsUpView = draggedHeadsUpView;
        updateHeadsUpScrim(false);
    }

    private float calculateHeadsUpAlpha() {
        Log.d(TAG, "calculateHeadsUpAlpha: ");
        float alpha;
        if (mPinnedHeadsUpCount >= 2) {
            alpha = 1.0f;
        } else if (mPinnedHeadsUpCount == 0) {
            alpha = 0.0f;
        } else {
            alpha = 1.0f - mTopHeadsUpDragAmount;
        }
        float expandFactor = (1.0f - mFraction);
        expandFactor = Math.max(expandFactor, 0.0f);
        return alpha * expandFactor;
    }

    public void forceHideScrims(boolean hide) {
        Log.d(TAG, "forceHideScrims: ");
        mForceHideScrims = hide;
        mAnimateChange = false;
        scheduleUpdate();
    }

    public void dontAnimateBouncerChangesUntilNextFrame() {
        Log.d(TAG, "dontAnimateBouncerChangesUntilNextFrame: ");
        mDontAnimateBouncerChanges = true;
    }
}
