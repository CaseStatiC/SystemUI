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
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import com.android.systemui.R;

/**
 * The guts of a notification revealed when performing a long press.
 */
public class NotificationGuts extends FrameLayout {

    public static final String TAG = "NotificationGuts";
    private Drawable mBackground;
    private int mClipTopAmount;
    private int mActualHeight;
    
    public NotificationGuts(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw: ");
        draw(canvas, mBackground);
    }

    private void draw(Canvas canvas, Drawable drawable) {
        Log.d(TAG, "draw: ");
        if (drawable != null) {
            drawable.setBounds(0, mClipTopAmount, getWidth(), mActualHeight);
            drawable.draw(canvas);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, "onFinishInflate: ");
        mBackground = mContext.getDrawable(R.drawable.notification_guts_bg);
        if (mBackground != null) {
            mBackground.setCallback(this);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        Log.d(TAG, "verifyDrawable: ");
        return super.verifyDrawable(who) || who == mBackground;
    }

    @Override
    protected void drawableStateChanged() {
        Log.d(TAG, "drawableStateChanged: ");
        drawableStateChanged(mBackground);
    }

    private void drawableStateChanged(Drawable d) {
        Log.d(TAG, "drawableStateChanged: ");
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        Log.d(TAG, "drawableHotspotChanged: ");
        if (mBackground != null) {
            mBackground.setHotspot(x, y);
        }
    }

    public void setActualHeight(int actualHeight) {
        Log.d(TAG, "setActualHeight: ");
        mActualHeight = actualHeight;
        invalidate();
    }

    public int getActualHeight() {
        Log.d(TAG, "getActualHeight: ");
        return mActualHeight;
    }

    public void setClipTopAmount(int clipTopAmount) {
        mClipTopAmount = clipTopAmount;
        invalidate();
    }

    @Override
    public boolean hasOverlappingRendering() {
        Log.d(TAG, "hasOverlappingRendering: ");
        // Prevents this view from creating a layer when alpha is animating.
        return false;
    }
}
