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

package com.android.systemui.statusbar.stack;

import android.util.Log;
import android.view.View;

import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.policy.HeadsUpManager;

import java.util.ArrayList;

/**
 * A global state to track all input states for the algorithm.
 */
public class AmbientState {
    public static final String TAG = "AmbientState";
    private ArrayList<View> mDraggedViews = new ArrayList<View>();
    private int mScrollY;
    private boolean mDimmed;
    private ActivatableNotificationView mActivatedChild;
    private float mOverScrollTopAmount;
    private float mOverScrollBottomAmount;
    private int mSpeedBumpIndex = -1;
    private boolean mDark;
    private boolean mHideSensitive;
    private HeadsUpManager mHeadsUpManager;
    private float mStackTranslation;
    private int mLayoutHeight;
    private int mTopPadding;
    private boolean mShadeExpanded;
    private float mMaxHeadsUpTranslation;
    private boolean mDismissAllInProgress;

    public int getScrollY() {
        Log.d(TAG, "getScrollY: ");
        return mScrollY;
    }

    public void setScrollY(int scrollY) {
        Log.d(TAG, "setScrollY: ");
        this.mScrollY = scrollY;
    }

    public void onBeginDrag(View view) {
        Log.d(TAG, "onBeginDrag: ");
        mDraggedViews.add(view);
    }

    public void onDragFinished(View view) {
        Log.d(TAG, "onDragFinished: ");
        mDraggedViews.remove(view);
    }

    public ArrayList<View> getDraggedViews() {
        Log.d(TAG, "getDraggedViews: ");
        return mDraggedViews;
    }

    /**
     * @param dimmed Whether we are in a dimmed state (on the lockscreen), where the backgrounds are
     *               translucent and everything is scaled back a bit.
     */
    public void setDimmed(boolean dimmed) {
        Log.d(TAG, "setDimmed: ");
        mDimmed = dimmed;
    }

    /** In dark mode, we draw as little as possible, assuming a black background */
    public void setDark(boolean dark) {
        Log.d(TAG, "setDark: ");
        mDark = dark;
    }

    public void setHideSensitive(boolean hideSensitive) {
        Log.d(TAG, "setHideSensitive: ");
        mHideSensitive = hideSensitive;
    }

    /**
     * In dimmed mode, a child can be activated, which happens on the first tap of the double-tap
     * interaction. This child is then scaled normally and its background is fully opaque.
     */
    public void setActivatedChild(ActivatableNotificationView activatedChild) {
        Log.d(TAG, "setActivatedChild: ");
        mActivatedChild = activatedChild;
    }

    public boolean isDimmed() {
        Log.d(TAG, "isDimmed: ");
        return mDimmed;
    }

    public boolean isDark() {
        Log.d(TAG, "isDark: ");
        return mDark;
    }

    public boolean isHideSensitive() {
        Log.d(TAG, "isHideSensitive: ");
        return mHideSensitive;
    }

    public ActivatableNotificationView getActivatedChild() {
        Log.d(TAG, "getActivatedChild: ");
        return mActivatedChild;
    }

    public void setOverScrollAmount(float amount, boolean onTop) {
        Log.d(TAG, "setOverScrollAmount: ");
        if (onTop) {
            mOverScrollTopAmount = amount;
        } else {
            mOverScrollBottomAmount = amount;
        }
    }

    public float getOverScrollAmount(boolean top) {
        Log.d(TAG, "getOverScrollAmount: ");
        return top ? mOverScrollTopAmount : mOverScrollBottomAmount;
    }

    public int getSpeedBumpIndex() {
        Log.d(TAG, "getSpeedBumpIndex: ");
        return mSpeedBumpIndex;
    }

    public void setSpeedBumpIndex(int speedBumpIndex) {
        Log.d(TAG, "setSpeedBumpIndex: ");
        mSpeedBumpIndex = speedBumpIndex;
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        Log.d(TAG, "setHeadsUpManager: ");
        mHeadsUpManager = headsUpManager;
    }

    public float getStackTranslation() {
        Log.d(TAG, "getStackTranslation: ");
        return mStackTranslation;
    }

    public void setStackTranslation(float stackTranslation) {
        Log.d(TAG, "setStackTranslation: ");
        mStackTranslation = stackTranslation;
    }

    public int getLayoutHeight() {
        Log.d(TAG, "getLayoutHeight: ");
        return mLayoutHeight;
    }

    public void setLayoutHeight(int layoutHeight) {
        Log.d(TAG, "setLayoutHeight: ");
        mLayoutHeight = layoutHeight;
    }

    public float getTopPadding() {
        Log.d(TAG, "getTopPadding: ");
        return mTopPadding;
    }

    public void setTopPadding(int topPadding) {
        Log.d(TAG, "setTopPadding: ");
        mTopPadding = topPadding;
    }

    public int getInnerHeight() {
        Log.d(TAG, "getInnerHeight: ");
        return mLayoutHeight - mTopPadding - getTopHeadsUpPushIn();
    }

    private int getTopHeadsUpPushIn() {
        Log.d(TAG, "getTopHeadsUpPushIn: ");
        ExpandableNotificationRow topHeadsUpEntry = getTopHeadsUpEntry();
        return topHeadsUpEntry != null ? topHeadsUpEntry.getHeadsUpHeight()
                - topHeadsUpEntry.getMinHeight(): 0;
    }

    public boolean isShadeExpanded() {
        Log.d(TAG, "isShadeExpanded: ");
        return mShadeExpanded;
    }

    public void setShadeExpanded(boolean shadeExpanded) {
        Log.d(TAG, "setShadeExpanded: ");
        mShadeExpanded = shadeExpanded;
    }

    public void setMaxHeadsUpTranslation(float maxHeadsUpTranslation) {
        Log.d(TAG, "setMaxHeadsUpTranslation: ");
        mMaxHeadsUpTranslation = maxHeadsUpTranslation;
    }

    public float getMaxHeadsUpTranslation() {
        Log.d(TAG, "getMaxHeadsUpTranslation: ");
        return mMaxHeadsUpTranslation;
    }

    public ExpandableNotificationRow getTopHeadsUpEntry() {
        Log.d(TAG, "getTopHeadsUpEntry: ");
        HeadsUpManager.HeadsUpEntry topEntry = mHeadsUpManager.getTopEntry();
        return topEntry == null ? null : topEntry.entry.row;
    }

    public void setDismissAllInProgress(boolean dismissAllInProgress) {
        Log.d(TAG, "setDismissAllInProgress: ");
        mDismissAllInProgress = dismissAllInProgress;
    }

    public boolean isDismissAllInProgress() {
        Log.d(TAG, "isDismissAllInProgress: ");
        return mDismissAllInProgress;
    }
}
