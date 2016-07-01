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
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.android.systemui.R;

/**
 * Like {@link ExpandableView}, but setting an outline for the height and clipping.
 */
public abstract class ExpandableOutlineView extends ExpandableView {
    public static final String TAG = "ExpandableOutlineView";
    private final Rect mOutlineRect = new Rect();
    protected final int mRoundedRectCornerRadius;
    private boolean mCustomOutline;
    private float mOutlineAlpha = 1f;

    public ExpandableOutlineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoundedRectCornerRadius = getResources().getDimensionPixelSize(
                R.dimen.notification_material_rounded_rect_radius);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (!mCustomOutline) {
                    outline.setRect(0,
                            mClipTopAmount,
                            getWidth(),
                            Math.max(getActualHeight(), mClipTopAmount));
                } else {
                    outline.setRoundRect(mOutlineRect, mRoundedRectCornerRadius);
                }
                outline.setAlpha(mOutlineAlpha);
            }
        });
    }

    @Override
    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        super.setActualHeight(actualHeight, notifyListeners);
        Log.d(TAG, "setActualHeight: ");
        invalidateOutline();
    }

    @Override
    public void setClipTopAmount(int clipTopAmount) {
        Log.d(TAG, "setClipTopAmount: ");
        super.setClipTopAmount(clipTopAmount);
        invalidateOutline();
    }

    protected void setOutlineAlpha(float alpha) {
        Log.d(TAG, "setOutlineAlpha: ");
        mOutlineAlpha = alpha;
        invalidateOutline();
    }

    protected void setOutlineRect(RectF rect) {
        Log.d(TAG, "setOutlineRect: ");
        if (rect != null) {
            setOutlineRect(rect.left, rect.top, rect.right, rect.bottom);
        } else {
            mCustomOutline = false;
            setClipToOutline(false);
            invalidateOutline();
        }
    }

    protected void setOutlineRect(float left, float top, float right, float bottom) {
        Log.d(TAG, "setOutlineRect: ");
        mCustomOutline = true;
        setClipToOutline(true);

        mOutlineRect.set((int) left, (int) top, (int) right, (int) bottom);

        // Outlines need to be at least 1 dp
        mOutlineRect.bottom = (int) Math.max(top, mOutlineRect.bottom);
        mOutlineRect.right = (int) Math.max(left, mOutlineRect.right);

        invalidateOutline();
    }

}
