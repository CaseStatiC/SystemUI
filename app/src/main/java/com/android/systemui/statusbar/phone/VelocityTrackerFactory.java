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
import android.util.Log;

import com.android.systemui.R;

/**
 * A class to generate {@link VelocityTrackerInterface}, depending on the configuration.
 */
public class VelocityTrackerFactory {

    public static final String TAG = "VelocityTrackerFactory";
    public static final String PLATFORM_IMPL = "platform";
    public static final String NOISY_IMPL = "noisy";

    public static VelocityTrackerInterface obtain(Context ctx) {
        Log.d(TAG, "obtain: ");
        String tracker = ctx.getResources().getString(R.string.velocity_tracker_impl);
        switch (tracker) {
            case NOISY_IMPL:
                return NoisyVelocityTracker.obtain();
            case PLATFORM_IMPL:
                return PlatformVelocityTracker.obtain();
            default:
                throw new IllegalStateException("Invalid tracker: " + tracker);
        }
    }
}
