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

package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.settings.CurrentUserTracker;

import java.util.ArrayList;

public final class KeyguardMonitor extends KeyguardUpdateMonitorCallback {

    private final ArrayList<Callback> mCallbacks = new ArrayList<Callback>();

    private final Context mContext;
    private final CurrentUserTracker mUserTracker;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;

    private int mCurrentUser;
    private boolean mShowing;
    private boolean mSecure;
    private boolean mCanSkipBouncer;

    private boolean mListening;
    public static final String TAG = "KeyguardUpdateMonitor";
    public KeyguardMonitor(Context context) {
        mContext = context;
        mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        mUserTracker = new CurrentUserTracker(mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                mCurrentUser = newUserId;
                updateCanSkipBouncerState();
            }
        };
    }

    public void addCallback(Callback callback) {
        Log.d(TAG, "addCallback: ");
        mCallbacks.add(callback);
        if (mCallbacks.size() != 0 && !mListening) {
            mListening = true;
            mCurrentUser = ActivityManager.getCurrentUser();
            updateCanSkipBouncerState();
            mKeyguardUpdateMonitor.registerCallback(this);
            mUserTracker.startTracking();
        }
    }

    public void removeCallback(Callback callback) {
        Log.d(TAG, "removeCallback: ");
        if (mCallbacks.remove(callback) && mCallbacks.size() == 0 && mListening) {
            mListening = false;
            mKeyguardUpdateMonitor.removeCallback(this);
            mUserTracker.stopTracking();
        }
    }

    public boolean isShowing() {
        Log.d(TAG, "isShowing: ");
        return mShowing;
    }

    public boolean isSecure() {
        Log.d(TAG, "isSecure: ");
        return mSecure;
    }

    public boolean canSkipBouncer() {
        Log.d(TAG, "canSkipBouncer: ");
        return mCanSkipBouncer;
    }

    public void notifyKeyguardState(boolean showing, boolean secure) {
        Log.d(TAG, "notifyKeyguardState: ");
        if (mShowing == showing && mSecure == secure) return;
        mShowing = showing;
        mSecure = secure;
        notifyKeyguardChanged();
    }

    @Override
    public void onTrustChanged(int userId) {
        Log.d(TAG, "onTrustChanged: ");
        updateCanSkipBouncerState();
        notifyKeyguardChanged();
    }

    private void updateCanSkipBouncerState() {
        Log.d(TAG, "updateCanSkipBouncerState: ");
        mCanSkipBouncer = mKeyguardUpdateMonitor.getUserCanSkipBouncer(mCurrentUser);
    }

    private void notifyKeyguardChanged() {
        Log.d(TAG, "notifyKeyguardChanged: ");
        for (Callback callback : mCallbacks) {
            callback.onKeyguardChanged();
        }
    }

    public interface Callback {
        void onKeyguardChanged();
    }
}