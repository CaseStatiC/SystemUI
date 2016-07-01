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
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * A view that can be used for both the dimmed and normal background of an notification.
 */
public class NotificationBackgroundView extends View {
    public static final String TAG = " NotificationBackgroundView";
    private Drawable mBackground;
    private int mClipTopAmount;
    private int mActualHeight;

    public NotificationBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw: ");
        draw(canvas, mBackground);
    }

    private void draw(Canvas canvas, Drawable drawable) {
        Log.d(TAG, "draw: ");
        if (drawable != null && mActualHeight > mClipTopAmount) {
            drawable.setBounds(0, mClipTopAmount, getWidth(), mActualHeight);
            drawable.draw(canvas);
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

    /**
     * Sets a background drawable. As we need to change our bounds independently of layout, we need
     * the notion of a background independently of the regular View background..
     */
    public void setCustomBackground(Drawable background) {
        Log.d(TAG, "setCustomBackground: ");
        if (mBackground != null) {
            mBackground.setCallback(null);
            unscheduleDrawable(mBackground);
        }
        mBackground = background;
        if (mBackground != null) {
            mBackground.setCallback(this);
        }
        if (mBackground instanceof RippleDrawable) {
            ((RippleDrawable) mBackground).setForceSoftware(true);
        }
        invalidate();
    }

    public void setCustomBackground(int drawableResId) {
        Log.d(TAG, "setCustomBackground: ");
        final Drawable d = mContext.getDrawable(drawableResId);
        setCustomBackground(d);
    }

    public void setTint(int tintColor) {
        Log.d(TAG, "setTint: ");
        if (tintColor != 0) {
            mBackground.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            mBackground.clearColorFilter();
        }
        invalidate();
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
        Log.d(TAG, "setClipTopAmount: ");
        mClipTopAmount = clipTopAmount;
        invalidate();
    }

    @Override
    public boolean hasOverlappingRendering() {
        Log.d(TAG, "hasOverlappingRendering: ");

        // Prevents this view from creating a layer when alpha is animating.
        return false;
    }

    public void setState(int[] drawableState) {
        Log.d(TAG, "setState: ");
        mBackground.setState(drawableState);
    }

    public void setRippleColor(int color) {
        Log.d(TAG, "setRippleColor: ");
        if (mBackground instanceof RippleDrawable) {
            RippleDrawable ripple = (RippleDrawable) mBackground;
            ripple.setColor(ColorStateList.valueOf(color));
        }
    }
}
