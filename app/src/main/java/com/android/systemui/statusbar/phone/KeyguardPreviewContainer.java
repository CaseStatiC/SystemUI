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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowInsets;
import android.widget.FrameLayout;

/**
 * A view group which contains the preview of phone/camera and draws a black bar at the bottom as
 * the fake navigation bar.
 */
public class KeyguardPreviewContainer extends FrameLayout {
    public static final String TAG = "KeyguardPreviewContainer";
    private Drawable mBlackBarDrawable = new Drawable() {
        @Override
        public void draw(Canvas canvas) {
            Log.d(TAG, "draw: ");
            canvas.save();
            canvas.clipRect(0, getHeight() - getPaddingBottom(), getWidth(), getHeight());
            canvas.drawColor(Color.BLACK);
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            Log.d(TAG, "setAlpha: ");
            // noop
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            Log.d(TAG, "setColorFilter: ");
            // noop
        }

        @Override
        public int getOpacity() {
            Log.d(TAG, "getOpacity: ");
            return android.graphics.PixelFormat.OPAQUE;
        }
    };

    public KeyguardPreviewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "KeyguardPreviewContainer: ");
        setBackground(mBlackBarDrawable);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        Log.d(TAG, "onApplyWindowInsets: ");
        setPadding(0, 0, 0, insets.getStableInsetBottom());
        return super.onApplyWindowInsets(insets);
    }
}
