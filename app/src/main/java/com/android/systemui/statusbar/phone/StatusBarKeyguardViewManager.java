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

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManagerGlobal;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.statusbar.CommandQueue;

import static com.android.keyguard.KeyguardHostView.OnDismissAction;

/**
 * Manages creating, showing, hiding and resetting the keyguard within the status bar. Calls back
 * via {@link ViewMediatorCallback} to poke the wake lock and report that the keyguard is done,
 * which is in turn, reported to this class by the current
 * {@link com.android.keyguard.KeyguardViewBase}.
 */
public class StatusBarKeyguardViewManager {

    // When hiding the Keyguard with timing supplied from WindowManager, better be early than late.
    private static final long HIDE_TIMING_CORRECTION_MS = -3 * 16;

    // Delay for showing the navigation bar when the bouncer appears. This should be kept in sync
    // with the appear animations of the PIN/pattern/password views.
    private static final long NAV_BAR_SHOW_DELAY_BOUNCER = 320;

    private static final long WAKE_AND_UNLOCK_SCRIM_FADEOUT_DURATION_MS = 200;

    private static String TAG = "StatusBarKeyguardViewManager";

    private final Context mContext;

    private LockPatternUtils mLockPatternUtils;
    private ViewMediatorCallback mViewMediatorCallback;
    private PhoneStatusBar mPhoneStatusBar;
    private ScrimController mScrimController;
    private FingerprintUnlockController mFingerprintUnlockController;

    private ViewGroup mContainer;
    private StatusBarWindowManager mStatusBarWindowManager;

    private boolean mDeviceInteractive = false;
    private boolean mScreenTurnedOn;
    private KeyguardBouncer mBouncer;
    private boolean mShowing;
    private boolean mOccluded;

    private boolean mFirstUpdate = true;
    private boolean mLastShowing;
    private boolean mLastOccluded;
    private boolean mLastBouncerShowing;
    private boolean mLastBouncerDismissible;
    private OnDismissAction mAfterKeyguardGoneAction;
    private boolean mDeviceWillWakeUp;
    private boolean mDeferScrimFadeOut;

    public StatusBarKeyguardViewManager(Context context, ViewMediatorCallback callback,
            LockPatternUtils lockPatternUtils) {
        mContext = context;
        mViewMediatorCallback = callback;
        mLockPatternUtils = lockPatternUtils;
    }

    public void registerStatusBar(PhoneStatusBar phoneStatusBar,
            ViewGroup container, StatusBarWindowManager statusBarWindowManager,
            ScrimController scrimController,
            FingerprintUnlockController fingerprintUnlockController) {
        Log.d(TAG, "registerStatusBar: ");
        mPhoneStatusBar = phoneStatusBar;
        mContainer = container;
        mStatusBarWindowManager = statusBarWindowManager;
        mScrimController = scrimController;
        mFingerprintUnlockController = fingerprintUnlockController;
        mBouncer = new KeyguardBouncer(mContext, mViewMediatorCallback, mLockPatternUtils,
                mStatusBarWindowManager, container);
    }

    /**
     * Show the keyguard.  Will handle creating and attaching to the view manager
     * lazily.
     */
    public void show(Bundle options) {
        Log.d(TAG, "show: ");
        mShowing = true;
        mStatusBarWindowManager.setKeyguardShowing(true);
        mScrimController.abortKeyguardFadingOut();
        reset();
    }

    /**
     * Shows the notification keyguard or the bouncer depending on
     * {@link KeyguardBouncer#needsFullscreenBouncer()}.
     */
    private void showBouncerOrKeyguard() {
        Log.d(TAG, "showBouncerOrKeyguard: ");
        if (mBouncer.needsFullscreenBouncer()) {

            // The keyguard might be showing (already). So we need to hide it.
            mPhoneStatusBar.hideKeyguard();
            mBouncer.show(true /* resetSecuritySelection */);
        } else {
            mPhoneStatusBar.showKeyguard();
            mBouncer.hide(false /* destroyView */);
            mBouncer.prepare();
        }
    }

    private void showBouncer() {
        Log.d(TAG, "showBouncer: ");
        if (mShowing) {
            mBouncer.show(false /* resetSecuritySelection */);
        }
        updateStates();
    }

    public void dismissWithAction(OnDismissAction r, Runnable cancelAction,
            boolean afterKeyguardGone) {
        Log.d(TAG, "dismissWithAction: ");
        if (mShowing) {
            if (!afterKeyguardGone) {
                mBouncer.showWithDismissAction(r, cancelAction);
            } else {
                mBouncer.show(false /* resetSecuritySelection */);
                mAfterKeyguardGoneAction = r;
            }
        }
        updateStates();
    }

    /**
     * Reset the state of the view.
     */
    public void reset() {
        Log.d(TAG, "reset: ");
        if (mShowing) {
            if (mOccluded) {
                mPhoneStatusBar.hideKeyguard();
                mPhoneStatusBar.stopWaitingForKeyguardExit();
                mBouncer.hide(false /* destroyView */);
            } else {
                showBouncerOrKeyguard();
            }
            KeyguardUpdateMonitor.getInstance(mContext).sendKeyguardReset();
            updateStates();
        }
    }

    public void onStartedGoingToSleep() {
        Log.d(TAG, "onStartedGoingToSleep: ");
        mPhoneStatusBar.onStartedGoingToSleep();
    }

    public void onFinishedGoingToSleep() {
        Log.d(TAG, "onFinishedGoingToSleep: ");
        mDeviceInteractive = false;
        mPhoneStatusBar.onFinishedGoingToSleep();
        mBouncer.onScreenTurnedOff();
    }

    public void onStartedWakingUp() {
        Log.d(TAG, "onStartedWakingUp: ");
        mDeviceInteractive = true;
        mDeviceWillWakeUp = false;
        mPhoneStatusBar.onStartedWakingUp();
    }

    public void onScreenTurningOn() {
        Log.d(TAG, "onScreenTurningOn: ");
        mPhoneStatusBar.onScreenTurningOn();
    }

    public boolean isScreenTurnedOn() {
        Log.d(TAG, "isScreenTurnedOn: ");
        return mScreenTurnedOn;
    }

    public void onScreenTurnedOn() {
        Log.d(TAG, "onScreenTurnedOn: ");
        mScreenTurnedOn = true;
        if (mDeferScrimFadeOut) {
            mDeferScrimFadeOut = false;
            animateScrimControllerKeyguardFadingOut(0, WAKE_AND_UNLOCK_SCRIM_FADEOUT_DURATION_MS,
                    true /* skipFirstFrame */);
            updateStates();
        }
        mPhoneStatusBar.onScreenTurnedOn();
    }

    public void onScreenTurnedOff() {
        Log.d(TAG, "onScreenTurnedOff: ");
        mScreenTurnedOn = false;
    }

    public void notifyDeviceWakeUpRequested() {
        Log.d(TAG, "notifyDeviceWakeUpRequested: ");
        mDeviceWillWakeUp = !mDeviceInteractive;
    }

    public void verifyUnlock() {
        Log.d(TAG, "verifyUnlock: ");
        dismiss();
    }

    public void setNeedsInput(boolean needsInput) {
        Log.d(TAG, "setNeedsInput: ");
        mStatusBarWindowManager.setKeyguardNeedsInput(needsInput);
    }

    public void setOccluded(boolean occluded) {
        Log.d(TAG, "setOccluded: ");
        if (occluded && !mOccluded && mShowing) {
            if (mPhoneStatusBar.isInLaunchTransition()) {
                mOccluded = true;
                mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(null /* beforeFading */,
                        new Runnable() {
                            @Override
                            public void run() {
                                mStatusBarWindowManager.setKeyguardOccluded(mOccluded);
                                reset();
                            }
                        });
                return;
            }
        }
        mOccluded = occluded;
        mStatusBarWindowManager.setKeyguardOccluded(occluded);
        reset();
    }

    public boolean isOccluded() {
        Log.d(TAG, "isOccluded: ");
        return mOccluded;
    }

    /**
     * Starts the animation before we dismiss Keyguard, i.e. an disappearing animation on the
     * security view of the bouncer.
     *
     * @param finishRunnable the runnable to be run after the animation finished, or {@code null} if
     *                       no action should be run
     */
    public void startPreHideAnimation(Runnable finishRunnable) {
        Log.d(TAG, "startPreHideAnimation: ");
        if (mBouncer.isShowing()) {
            mBouncer.startPreHideAnimation(finishRunnable);
        } else if (finishRunnable != null) {
            finishRunnable.run();
        }
    }

    /**
     * Hides the keyguard view
     */
    public void hide(long startTime, final long fadeoutDuration) {
        Log.d(TAG, "hide: ");
        mShowing = false;

        long uptimeMillis = SystemClock.uptimeMillis();
        long delay = Math.max(0, startTime + HIDE_TIMING_CORRECTION_MS - uptimeMillis);

        if (mPhoneStatusBar.isInLaunchTransition() ) {
            mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(new Runnable() {
                @Override
                public void run() {
                    mStatusBarWindowManager.setKeyguardShowing(false);
                    mStatusBarWindowManager.setKeyguardFadingAway(true);
                    mBouncer.hide(true /* destroyView */);
                    updateStates();
                    mScrimController.animateKeyguardFadingOut(
                            PhoneStatusBar.FADE_KEYGUARD_START_DELAY,
                            PhoneStatusBar.FADE_KEYGUARD_DURATION, null,
                            false /* skipFirstFrame */);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    mPhoneStatusBar.hideKeyguard();
                    mStatusBarWindowManager.setKeyguardFadingAway(false);
                    mViewMediatorCallback.keyguardGone();
                    executeAfterKeyguardGoneAction();
                }
            });
        } else {
            if (mFingerprintUnlockController.getMode()
                    == FingerprintUnlockController.MODE_WAKE_AND_UNLOCK_PULSING) {
                mFingerprintUnlockController.startKeyguardFadingAway();
                mPhoneStatusBar.setKeyguardFadingAway(startTime, 0, 240);
                mStatusBarWindowManager.setKeyguardFadingAway(true);
                mPhoneStatusBar.fadeKeyguardWhilePulsing();
                animateScrimControllerKeyguardFadingOut(0, 240, new Runnable() {
                    @Override
                    public void run() {
                        mPhoneStatusBar.hideKeyguard();
                    }
                }, false /* skipFirstFrame */);
            } else {
                mFingerprintUnlockController.startKeyguardFadingAway();
                mPhoneStatusBar.setKeyguardFadingAway(startTime, delay, fadeoutDuration);
                boolean staying = mPhoneStatusBar.hideKeyguard();
                if (!staying) {
                    mStatusBarWindowManager.setKeyguardFadingAway(true);
                    if (mFingerprintUnlockController.getMode()
                            == FingerprintUnlockController.MODE_WAKE_AND_UNLOCK) {
                        if (!mScreenTurnedOn) {
                            mDeferScrimFadeOut = true;
                        } else {

                            // Screen is already on, don't defer with fading out.
                            animateScrimControllerKeyguardFadingOut(0,
                                    WAKE_AND_UNLOCK_SCRIM_FADEOUT_DURATION_MS,
                                    true /* skipFirstFrame */);
                        }
                    } else {
                        animateScrimControllerKeyguardFadingOut(delay, fadeoutDuration,
                                false /* skipFirstFrame */);
                    }
                } else {
                    mScrimController.animateGoingToFullShade(delay, fadeoutDuration);
                    mPhoneStatusBar.finishKeyguardFadingAway();
                }
            }
            mStatusBarWindowManager.setKeyguardShowing(false);
            mBouncer.hide(true /* destroyView */);
            mViewMediatorCallback.keyguardGone();
            executeAfterKeyguardGoneAction();
            updateStates();
        }
    }

    private void animateScrimControllerKeyguardFadingOut(long delay, long duration,
            boolean skipFirstFrame) {
        Log.d(TAG, "animateScrimControllerKeyguardFadingOut: ");
        animateScrimControllerKeyguardFadingOut(delay, duration, null /* endRunnable */,
                skipFirstFrame);
    }

    private void animateScrimControllerKeyguardFadingOut(long delay, long duration,
            final Runnable endRunnable, boolean skipFirstFrame) {
        Log.d(TAG, "animateScrimControllerKeyguardFadingOut: ");
        Trace.asyncTraceBegin(Trace.TRACE_TAG_VIEW, "Fading out", 0);
        mScrimController.animateKeyguardFadingOut(delay, duration, new Runnable() {
            @Override
            public void run() {
                if (endRunnable != null) {
                    endRunnable.run();
                }
                mStatusBarWindowManager.setKeyguardFadingAway(false);
                mPhoneStatusBar.finishKeyguardFadingAway();
                mFingerprintUnlockController.finishKeyguardFadingAway();
                WindowManagerGlobal.getInstance().trimMemory(
                        ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
                Trace.asyncTraceEnd(Trace.TRACE_TAG_VIEW, "Fading out", 0);
            }
        }, skipFirstFrame);
    }

    private void executeAfterKeyguardGoneAction() {
        Log.d(TAG, "executeAfterKeyguardGoneAction: ");
        if (mAfterKeyguardGoneAction != null) {
            mAfterKeyguardGoneAction.onDismiss();
            mAfterKeyguardGoneAction = null;
        }
    }

    /**
     * Dismisses the keyguard by going to the next screen or making it gone.
     */
    public void dismiss() {
        Log.d(TAG, "dismiss: ");
        if (mDeviceInteractive || mDeviceWillWakeUp) {
            showBouncer();
        }
    }

    /**
     * WARNING: This method might cause Binder calls.
     */
    public boolean isSecure() {
        Log.d(TAG, "isSecure: ");
        return mBouncer.isSecure();
    }

    /**
     * @return Whether the keyguard is showing
     */
    public boolean isShowing() {
        Log.d(TAG, "isShowing: ");
        return mShowing;
    }

    /**
     * Notifies this manager that the back button has been pressed.
     *
     * @return whether the back press has been handled
     */
    public boolean onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        if (mBouncer.isShowing()) {
            mPhoneStatusBar.endAffordanceLaunch();
            reset();
            return true;
        }
        return false;
    }

    public boolean isBouncerShowing() {
        Log.d(TAG, "isBouncerShowing: ");
        return mBouncer.isShowing();
    }

    private long getNavBarShowDelay() {
        Log.d(TAG, "getNavBarShowDelay: ");
        if (mPhoneStatusBar.isKeyguardFadingAway()) {
            return mPhoneStatusBar.getKeyguardFadingAwayDelay();
        } else {

            // Keyguard is not going away, thus we are showing the navigation bar because the
            // bouncer is appearing.
            return NAV_BAR_SHOW_DELAY_BOUNCER;
        }
    }

    private Runnable mMakeNavigationBarVisibleRunnable = new Runnable() {
        @Override
        public void run() {
            mPhoneStatusBar.getNavigationBarView().setVisibility(View.VISIBLE);
        }
    };

    private void updateStates() {
        Log.d(TAG, "updateStates: ");
        int vis = mContainer.getSystemUiVisibility();
        boolean showing = mShowing;
        boolean occluded = mOccluded;
        boolean bouncerShowing = mBouncer.isShowing();
        boolean bouncerDismissible = !mBouncer.isFullscreenBouncer();

        if ((bouncerDismissible || !showing) != (mLastBouncerDismissible || !mLastShowing)
                || mFirstUpdate) {
            if (bouncerDismissible || !showing) {
                mContainer.setSystemUiVisibility(vis & ~View.STATUS_BAR_DISABLE_BACK);
            } else {
                mContainer.setSystemUiVisibility(vis | View.STATUS_BAR_DISABLE_BACK);
            }
        }

        boolean navBarVisible = (!(showing && !occluded) || bouncerShowing);
        boolean lastNavBarVisible = (!(mLastShowing && !mLastOccluded) || mLastBouncerShowing);
        if (navBarVisible != lastNavBarVisible || mFirstUpdate) {
            if (mPhoneStatusBar.getNavigationBarView() != null) {
                if (navBarVisible) {
                    long delay = getNavBarShowDelay();
                    if (delay == 0) {
                        mMakeNavigationBarVisibleRunnable.run();
                    } else {
                        mContainer.postOnAnimationDelayed(mMakeNavigationBarVisibleRunnable,
                                delay);
                    }
                } else {
                    mContainer.removeCallbacks(mMakeNavigationBarVisibleRunnable);
                    mPhoneStatusBar.getNavigationBarView().setVisibility(View.GONE);
                }
            }
        }

        if (bouncerShowing != mLastBouncerShowing || mFirstUpdate) {
            mStatusBarWindowManager.setBouncerShowing(bouncerShowing);
            mPhoneStatusBar.setBouncerShowing(bouncerShowing);
            mScrimController.setBouncerShowing(bouncerShowing);
        }

        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        if ((showing && !occluded) != (mLastShowing && !mLastOccluded) || mFirstUpdate) {
            updateMonitor.onKeyguardVisibilityChanged(showing && !occluded);
        }
        if (bouncerShowing != mLastBouncerShowing || mFirstUpdate) {
            updateMonitor.sendKeyguardBouncerChanged(bouncerShowing);
        }

        mFirstUpdate = false;
        mLastShowing = showing;
        mLastOccluded = occluded;
        mLastBouncerShowing = bouncerShowing;
        mLastBouncerDismissible = bouncerDismissible;

        mPhoneStatusBar.onKeyguardViewManagerStatesUpdated();
    }

    public boolean onMenuPressed() {
        Log.d(TAG, "onMenuPressed: ");
        return mBouncer.onMenuPressed();
    }

    public boolean interceptMediaKey(KeyEvent event) {
        Log.d(TAG, "interceptMediaKey: ");
        return mBouncer.interceptMediaKey(event);
    }

    public void onActivityDrawn() {
        Log.d(TAG, "onActivityDrawn: ");
        if (mPhoneStatusBar.isCollapsing()) {
            mPhoneStatusBar.addPostCollapseAction(new Runnable() {
                @Override
                public void run() {
                    mViewMediatorCallback.readyForKeyguardDone();
                }
            });
        } else {
            mViewMediatorCallback.readyForKeyguardDone();
        }
    }

    public boolean shouldDisableWindowAnimationsForUnlock() {
        Log.d(TAG, "shouldDisableWindowAnimationsForUnlock: ");
        return mPhoneStatusBar.isInLaunchTransition();
    }

    public boolean isGoingToNotificationShade() {
        Log.d(TAG, "isGoingToNotificationShade: ");
        return mPhoneStatusBar.isGoingToNotificationShade();
    }

    public boolean isSecure(int userId) {
        Log.d(TAG, "isSecure: ");
        return mBouncer.isSecure() || mLockPatternUtils.isSecure(userId);
    }

    public boolean isInputRestricted() {
        Log.d(TAG, "isInputRestricted: ");
        return mViewMediatorCallback.isInputRestricted();
    }

    public void keyguardGoingAway() {
        Log.d(TAG, "keyguardGoingAway: ");
        mPhoneStatusBar.keyguardGoingAway();
    }

    public void animateCollapsePanels(float speedUpFactor) {
        Log.d(TAG, "animateCollapsePanels: ");
        mPhoneStatusBar.animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE, true /* force */,
                false /* delayed */, speedUpFactor);
    }

    /**
     * Notifies that the user has authenticated by other means than using the bouncer, for example,
     * fingerprint.
     */
    public void notifyKeyguardAuthenticated(boolean strongAuth) {
        Log.d(TAG, "notifyKeyguardAuthenticated: ");
        mBouncer.notifyKeyguardAuthenticated(strongAuth);
    }

    public void showBouncerMessage(String message, int color) {
        Log.d(TAG, "showBouncerMessage: ");
        mBouncer.showMessage(message, color);
    }

    public ViewRootImpl getViewRootImpl() {
        Log.d(TAG, "getViewRootImpl: ");
        return mPhoneStatusBar.getStatusBarView().getViewRootImpl();
    }
}
