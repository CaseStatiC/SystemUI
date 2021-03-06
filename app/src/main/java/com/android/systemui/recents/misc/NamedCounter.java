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
 * limitations under the License.
 */

package com.android.systemui.recents.misc;

import android.util.Log;
/**
 * Used to generate successive incremented names.
 */
public class NamedCounter {

    public static final String TAG = "NamedCounter";
    int mCount;
    String mPrefix = "";
    String mSuffix = "";

    public NamedCounter(String prefix, String suffix) {
        mPrefix = prefix;
        mSuffix = suffix;
    }

    /** Returns the next name. */
    public String nextName() {
        Log.d(TAG, "nextName: ");
        String name = mPrefix + mCount + mSuffix;
        mCount++;
        return name;
    }
}
