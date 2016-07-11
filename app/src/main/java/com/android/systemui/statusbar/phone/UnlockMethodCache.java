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

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;

import java.util.ArrayList;

/**
 * Caches whether the current unlock method is insecure, taking trust into account. This information
 * might be a little bit out of date and should not be used for actual security decisions; it should
 * be only used for visual indications.
 */
public class UnlockMethodCache {

    public static final String TAG = "UnlockMethodCache";
    private static UnlockMethodCache sInstance;

    private final LockPatternUtils mLockPatternUtils;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final ArrayList<OnUnlockMethodChangedListener> mListeners = new ArrayList<>();
    /** Whether the user configured a secure unlock method (PIN, password, etc.) */
    private boolean mSecure;
    /** Whether the unlock method is currently insecure (insecure method or trusted environment) */
    private boolean mCanSkipBouncer;
    private boolean mTrustManaged;
    private boolean mFaceUnlockRunning;
    private boolean mTrusted;

    private UnlockMethodCache(Context ctx) {
        mLockPatternUtils = new LockPatternUtils(ctx);
        mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(ctx);
        KeyguardUpdateMonitor.getInstance(ctx).registerCallback(mCallback);
        update(true /* updateAlways */);
    }

    public static UnlockMethodCache getInstance(Context context) {
        Log.d(TAG, "getInstance: ");
        if (sInstance == null) {
            sInstance = new UnlockMethodCache(context);
        }
        return sInstance;
    }

    /**
     * @return whether the user configured a secure unlock method like PIN, password, etc.
     */
    public boolean isMethodSecure() {
        Log.d(TAG, "isMethodSecure: ");
        return mSecure;
    }

    public boolean isTrusted() {
        Log.d(TAG, "isTrusted: ");
        return mTrusted;
    }

    /**
     * @return whether the lockscreen is currently insecure, and the bouncer won't be shown
     */
    public boolean canSkipBouncer() {
        Log.d(TAG, "canSkipBouncer: ");
        return mCanSkipBouncer;
    }

    public void addListener(OnUnlockMethodChangedListener listener) {
        Log.d(TAG, "addListener: ");
        mListeners.add(listener);
    }

    public void removeListener(OnUnlockMethodChangedListener listener) {
        Log.d(TAG, "removeListener: ");
        mListeners.remove(listener);
    }

    private void update(boolean updateAlways) {
        Log.d(TAG, "update: ");
        int user = KeyguardUpdateMonitor.getCurrentUser();
        boolean secure = mLockPatternUtils.isSecure(user);
        boolean canSkipBouncer = !secure ||  mKeyguardUpdateMonitor.getUserCanSkipBouncer(user);
        boolean trustManaged = mKeyguardUpdateMonitor.getUserTrustIsManaged(user);
        boolean trusted = mKeyguardUpdateMonitor.getUserHasTrust(user);
        boolean faceUnlockRunning = mKeyguardUpdateMonitor.isFaceUnlockRunning(user)
                && trustManaged;
        boolean changed = secure != mSecure || canSkipBouncer != mCanSkipBouncer ||
                trustManaged != mTrustManaged  || faceUnlockRunning != mFaceUnlockRunning;
        if (changed || updateAlways) {
            mSecure = secure;
            mCanSkipBouncer = canSkipBouncer;
            mTrusted = trusted;
            mTrustManaged = trustManaged;
            mFaceUnlockRunning = faceUnlockRunning;
            notifyListeners();
        }
    }

    private void notifyListeners() {
        Log.d(TAG, "notifyListeners: ");
        for (OnUnlockMethodChangedListener listener : mListeners) {
            listener.onUnlockMethodStateChanged();
        }
    }

    private final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onUserSwitchComplete(int userId) {
            Log.d(TAG, "mCallback: onUserSwitchComplete: ");
            update(false /* updateAlways */);
        }

        @Override
        public void onTrustChanged(int userId) {
            Log.d(TAG, "mCallback: onTrustChanged: ");
            update(false /* updateAlways */);
        }

        @Override
        public void onTrustManagedChanged(int userId) {
            Log.d(TAG, "mCallback: onTrustManagedChanged: ");
            update(false /* updateAlways */);
        }

        @Override
        public void onStartedWakingUp() {
            Log.d(TAG, "mCallback: onStartedWakingUp: ");
            update(false /* updateAlways */);
        }

        @Override
        public void onFingerprintAuthenticated(int userId) {
            Log.d(TAG, "mCallback: onFingerprintAuthenticated: ");
            if (!mKeyguardUpdateMonitor.isUnlockingWithFingerprintAllowed()) {
                return;
            }
            update(false /* updateAlways */);
        }

        @Override
        public void onFaceUnlockStateChanged(boolean running, int userId) {
            Log.d(TAG, "mCallback: onFaceUnlockStateChanged: ");
            update(false /* updateAlways */);
        }

        @Override
        public void onStrongAuthStateChanged(int userId) {
            update(false /* updateAlways */);
        }
    };

    public boolean isTrustManaged() {
        Log.d(TAG, "isTrustManaged: ");
        return mTrustManaged;
    }

    public boolean isFaceUnlockRunning() {
        Log.d(TAG, "isFaceUnlockRunning: ");
        return mFaceUnlockRunning;
    }

    public static interface OnUnlockMethodChangedListener {
        void onUnlockMethodStateChanged();
    }
}
