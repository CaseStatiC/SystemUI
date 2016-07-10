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

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.android.keyguard.R;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.StatusBarState;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;

/**
 * Encapsulates all logic for the status bar window state management.
 */
public class StatusBarWindowManager {

    public static final String TAG = "StatusBarWindowManager";
    private final Context mContext;
    private final WindowManager mWindowManager;
    private View mStatusBarView;
    private WindowManager.LayoutParams mLp;
    private WindowManager.LayoutParams mLpChanged;
    private int mBarHeight;
    private final boolean mKeyguardScreenRotation;
    private final float mScreenBrightnessDoze;
    private final State mCurrentState = new State();

    public StatusBarWindowManager(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mKeyguardScreenRotation = shouldEnableKeyguardScreenRotation();
        mScreenBrightnessDoze = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_screenBrightnessDoze) / 255f;
    }

    private boolean shouldEnableKeyguardScreenRotation() {
        Log.d(TAG, "shouldEnableKeyguardScreenRotation: ");
        Resources res = mContext.getResources();
        return SystemProperties.getBoolean("lockscreen.rot_override", false)
                || res.getBoolean(R.bool.config_enableLockScreenRotation);
    }

    /**
     * Adds the status bar view to the window manager.
     *
     * @param statusBarView The view to add.
     * @param barHeight The height of the status bar in collapsed state.
     */
    public void add(View statusBarView, int barHeight) {
        Log.d(TAG, "add: ");

        // Now that the status bar window encompasses the sliding panel and its
        // translucent backdrop, the entire thing is made TRANSLUCENT and is
        // hardware-accelerated.
        mLp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                barHeight,
                WindowManager.LayoutParams.TYPE_STATUS_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                PixelFormat.TRANSLUCENT);
        mLp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mLp.gravity = Gravity.TOP;
        mLp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        mLp.setTitle("StatusBar");
        mLp.packageName = mContext.getPackageName();
        mStatusBarView = statusBarView;
        mBarHeight = barHeight;
        mWindowManager.addView(mStatusBarView, mLp);
        mLpChanged = new WindowManager.LayoutParams();
        mLpChanged.copyFrom(mLp);
    }

    private void applyKeyguardFlags(State state) {
        Log.d(TAG, "applyKeyguardFlags: ");
        if (state.keyguardShowing) {
            mLpChanged.flags |= WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
            mLpChanged.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_KEYGUARD;
        } else {
            mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
            mLpChanged.privateFlags &= ~WindowManager.LayoutParams.PRIVATE_FLAG_KEYGUARD;
        }
    }

    private void adjustScreenOrientation(State state) {
        Log.d(TAG, "adjustScreenOrientation: ");
        if (state.isKeyguardShowingAndNotOccluded()) {
            if (mKeyguardScreenRotation) {
                mLpChanged.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
            } else {
                mLpChanged.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
            }
        } else {
            mLpChanged.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
    }

    private void applyFocusableFlag(State state) {
        Log.d(TAG, "applyFocusableFlag: ");
        boolean panelFocusable = state.statusBarFocusable && state.panelExpanded;
        if (state.keyguardShowing && state.keyguardNeedsInput && state.bouncerShowing) {
            mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        } else if (state.isKeyguardShowingAndNotOccluded() || panelFocusable) {
            mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            mLpChanged.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        } else {
            mLpChanged.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        }
    }

    private void applyHeight(State state) {
        Log.d(TAG, "applyHeight: ");
        boolean expanded = isExpanded(state);
        if (expanded) {
            mLpChanged.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            mLpChanged.height = mBarHeight;
        }
    }

    private boolean isExpanded(State state) {
        Log.d(TAG, "isExpanded: ");
        return !state.forceCollapsed && (state.isKeyguardShowingAndNotOccluded()
                || state.panelVisible || state.keyguardFadingAway || state.bouncerShowing
                || state.headsUpShowing);
    }

    private void applyFitsSystemWindows(State state) {
        Log.d(TAG, "applyFitsSystemWindows: ");
        mStatusBarView.setFitsSystemWindows(!state.isKeyguardShowingAndNotOccluded());
    }

    private void applyUserActivityTimeout(State state) {
        Log.d(TAG, "applyUserActivityTimeout: ");
        if (state.isKeyguardShowingAndNotOccluded()
                && state.statusBarState == StatusBarState.KEYGUARD
                && !state.qsExpanded) {
            mLpChanged.userActivityTimeout = KeyguardViewMediator.AWAKE_INTERVAL_DEFAULT_MS;
        } else {
            mLpChanged.userActivityTimeout = -1;
        }
    }

    private void applyInputFeatures(State state) {
        Log.d(TAG, "applyInputFeatures: ");
        if (state.isKeyguardShowingAndNotOccluded()
                && state.statusBarState == StatusBarState.KEYGUARD
                && !state.qsExpanded) {
            mLpChanged.inputFeatures |=
                    WindowManager.LayoutParams.INPUT_FEATURE_DISABLE_USER_ACTIVITY;
        } else {
            mLpChanged.inputFeatures &=
                    ~WindowManager.LayoutParams.INPUT_FEATURE_DISABLE_USER_ACTIVITY;
        }
    }

    private void apply(State state) {
        Log.d(TAG, "apply: ");
        applyKeyguardFlags(state);
        applyForceStatusBarVisibleFlag(state);
        applyFocusableFlag(state);
        adjustScreenOrientation(state);
        applyHeight(state);
        applyUserActivityTimeout(state);
        applyInputFeatures(state);
        applyFitsSystemWindows(state);
        applyModalFlag(state);
        applyBrightness(state);
        if (mLp.copyFrom(mLpChanged) != 0) {
            mWindowManager.updateViewLayout(mStatusBarView, mLp);
        }
    }

    private void applyForceStatusBarVisibleFlag(State state) {
        Log.d(TAG, "applyForceStatusBarVisibleFlag: ");
        if (state.forceStatusBarVisible) {
            mLpChanged.privateFlags |= WindowManager
                    .LayoutParams.PRIVATE_FLAG_FORCE_STATUS_BAR_VISIBLE_TRANSPARENT;
        } else {
            mLpChanged.privateFlags &= ~WindowManager
                    .LayoutParams.PRIVATE_FLAG_FORCE_STATUS_BAR_VISIBLE_TRANSPARENT;
        }
    }

    private void applyModalFlag(State state) {
        Log.d(TAG, "applyModalFlag: ");
        if (state.headsUpShowing) {
            mLpChanged.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        } else {
            mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }
    }

    private void applyBrightness(State state) {
        Log.d(TAG, "applyBrightness: ");
        if (state.forceDozeBrightness) {
            mLpChanged.screenBrightness = mScreenBrightnessDoze;
        } else {
            mLpChanged.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }
    }

    public void setKeyguardShowing(boolean showing) {
        Log.d(TAG, "setKeyguardShowing: ");
        mCurrentState.keyguardShowing = showing;
        apply(mCurrentState);
    }

    public void setKeyguardOccluded(boolean occluded) {
        Log.d(TAG, "setKeyguardOccluded: ");
        mCurrentState.keyguardOccluded = occluded;
        apply(mCurrentState);
    }

    public void setKeyguardNeedsInput(boolean needsInput) {
        Log.d(TAG, "setKeyguardNeedsInput: ");
        mCurrentState.keyguardNeedsInput = needsInput;
        apply(mCurrentState);
    }

    public void setPanelVisible(boolean visible) {
        Log.d(TAG, "setPanelVisible: ");
        mCurrentState.panelVisible = visible;
        mCurrentState.statusBarFocusable = visible;
        apply(mCurrentState);
    }

    public void setStatusBarFocusable(boolean focusable) {
        Log.d(TAG, "setStatusBarFocusable: ");
        mCurrentState.statusBarFocusable = focusable;
        apply(mCurrentState);
    }

    public void setBouncerShowing(boolean showing) {
        Log.d(TAG, "setBouncerShowing: ");
        mCurrentState.bouncerShowing = showing;
        apply(mCurrentState);
    }

    public void setKeyguardFadingAway(boolean keyguardFadingAway) {
        Log.d(TAG, "setKeyguardFadingAway: ");
        mCurrentState.keyguardFadingAway = keyguardFadingAway;
        apply(mCurrentState);
    }

    public void setQsExpanded(boolean expanded) {
        Log.d(TAG, "setQsExpanded: ");
        mCurrentState.qsExpanded = expanded;
        apply(mCurrentState);
    }

    public void setHeadsUpShowing(boolean showing) {
        Log.d(TAG, "setHeadsUpShowing: ");
        mCurrentState.headsUpShowing = showing;
        apply(mCurrentState);
    }

    /**
     * @param state The {@link StatusBarState} of the status bar.
     */
    public void setStatusBarState(int state) {
        Log.d(TAG, "setStatusBarState: ");
        mCurrentState.statusBarState = state;
        apply(mCurrentState);
    }

    public void setForceStatusBarVisible(boolean forceStatusBarVisible) {
        Log.d(TAG, "setForceStatusBarVisible: ");
        mCurrentState.forceStatusBarVisible = forceStatusBarVisible;
        apply(mCurrentState);
    }

    /**
     * Force the window to be collapsed, even if it should theoretically be expanded.
     * Used for when a heads-up comes in but we still need to wait for the touchable regions to
     * be computed.
     */
    public void setForceWindowCollapsed(boolean force) {
        Log.d(TAG, "setForceWindowCollapsed: ");
        mCurrentState.forceCollapsed = force;
        apply(mCurrentState);
    }

    public void setPanelExpanded(boolean isExpanded) {
        Log.d(TAG, "setPanelExpanded: ");
        mCurrentState.panelExpanded = isExpanded;
        apply(mCurrentState);
    }

    /**
     * Set whether the screen brightness is forced to the value we use for doze mode by the status
     * bar window.
     */
    public void setForceDozeBrightness(boolean forceDozeBrightness) {
        Log.d(TAG, "setForceDozeBrightness: ");
        mCurrentState.forceDozeBrightness = forceDozeBrightness;
        apply(mCurrentState);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Log.d(TAG, "dump: ");
        pw.println("StatusBarWindowManager state:");
        pw.println(mCurrentState);
    }

    private static class State {
        boolean keyguardShowing;
        boolean keyguardOccluded;
        boolean keyguardNeedsInput;
        boolean panelVisible;
        boolean panelExpanded;
        boolean statusBarFocusable;
        boolean bouncerShowing;
        boolean keyguardFadingAway;
        boolean qsExpanded;
        boolean headsUpShowing;
        boolean forceStatusBarVisible;
        boolean forceCollapsed;
        boolean forceDozeBrightness;

        /**
         * The {@link BaseStatusBar} state from the status bar.
         */
        int statusBarState;

        private boolean isKeyguardShowingAndNotOccluded() {
            return keyguardShowing && !keyguardOccluded;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            String newLine = "\n";
            result.append("Window State {");
            result.append(newLine);

            Field[] fields = this.getClass().getDeclaredFields();

            // Print field names paired with their values
            for (Field field : fields) {
                result.append("  ");
                try {
                    result.append(field.getName());
                    result.append(": ");
                    //requires access to private field:
                    result.append(field.get(this));
                } catch (IllegalAccessException ex) {
                }
                result.append(newLine);
            }
            result.append("}");

            return result.toString();
        }
    }
}
