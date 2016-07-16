/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;

public abstract class SystemUI {
    public static final String TAG = "SystemUI";
    public Context mContext;
    public Map<Class<?>, Object> mComponents;

    public abstract void start();

    protected void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged: ");
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Log.d(TAG, "dump: ");
    }

    protected void onBootCompleted() {
        Log.d(TAG, "onBootCompleted: ");
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> interfaceType) {
        Log.d(TAG, "getComponent: ");
        return (T) (mComponents != null ? mComponents.get(interfaceType) : null);
    }

    public <T, C extends T> void putComponent(Class<T> interfaceType, C component) {
        Log.d(TAG, "putComponent: ");
        if (mComponents != null) {
            mComponents.put(interfaceType, component);
        }
    }
}
