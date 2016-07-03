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

package com.android.systemui.keyguard;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.systemui.SystemUIApplication;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class KeyguardService extends Service {
    static final String TAG = "KeyguardService";
    static final String PERMISSION = android.Manifest.permission.CONTROL_KEYGUARD;

    private KeyguardViewMediator mKeyguardViewMediator;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
        mKeyguardViewMediator =
                ((SystemUIApplication) getApplication()).getComponent(KeyguardViewMediator.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: ");
        return mBinder;
    }

    void checkPermission() {
        Log.d(TAG, "checkPermission: ");
        // Avoid deadlock by avoiding calling back into the system process.
        if (Binder.getCallingUid() == Process.SYSTEM_UID) return;

        // Otherwise,explicitly check for caller permission ...
        if (getBaseContext().checkCallingOrSelfPermission(PERMISSION) != PERMISSION_GRANTED) {
            Log.w(TAG, "Caller needs permission '" + PERMISSION + "' to call " + Debug.getCaller());
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid()
                    + ", must have permission " + PERMISSION);
        }
    }

    private final IKeyguardService.Stub mBinder = new IKeyguardService.Stub() {

        @Override // Binder interface
        public void addStateMonitorCallback(IKeyguardStateCallback callback) {
            Log.d(TAG, "mBinder: addStateMonitorCallback: ");
            checkPermission();
            mKeyguardViewMediator.addStateMonitorCallback(callback);
        }

        @Override // Binder interface
        public void verifyUnlock(IKeyguardExitCallback callback) {
            Log.d(TAG, "mBinder: verifyUnlock: ");
            checkPermission();
            mKeyguardViewMediator.verifyUnlock(callback);
        }

        @Override // Binder interface
        public void keyguardDone(boolean authenticated, boolean wakeup) {
            Log.d(TAG, "mBinder: keyguardDone: ");
            checkPermission();
            // TODO: Remove wakeup
            mKeyguardViewMediator.keyguardDone(authenticated);
        }

        @Override // Binder interface
        public void setOccluded(boolean isOccluded) {
            Log.d(TAG, "mBinder: setOccluded: ");
            checkPermission();
            mKeyguardViewMediator.setOccluded(isOccluded);
        }

        @Override // Binder interface
        public void dismiss() {
            Log.d(TAG, "mBinder: dismiss: ");
            checkPermission();
            mKeyguardViewMediator.dismiss();
        }

        @Override // Binder interface
        public void onDreamingStarted() {
            Log.d(TAG, "mBinder: onDreamingStarted: ");
            checkPermission();
            mKeyguardViewMediator.onDreamingStarted();
        }

        @Override // Binder interface
        public void onDreamingStopped() {
            Log.d(TAG, "mBinder: onDreamingStopped: ");
            checkPermission();
            mKeyguardViewMediator.onDreamingStopped();
        }

        @Override // Binder interface
        public void onStartedGoingToSleep(int reason) {
            Log.d(TAG, "mBinder: onStartedGoingToSleep: ");
            checkPermission();
            mKeyguardViewMediator.onStartedGoingToSleep(reason);
        }

        @Override // Binder interface
        public void onFinishedGoingToSleep(int reason) {
            Log.d(TAG, "mBinder: onFinishedGoingToSleep: ");
            checkPermission();
            mKeyguardViewMediator.onFinishedGoingToSleep(reason);
        }

        @Override // Binder interface
        public void onStartedWakingUp() {
            Log.d(TAG, "mBinder: onStartedWakingUp: ");
            checkPermission();
            mKeyguardViewMediator.onStartedWakingUp();
        }

        @Override // Binder interface
        public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
            Log.d(TAG, "mBinder: onScreenTurningOn: ");
            checkPermission();
            mKeyguardViewMediator.onScreenTurningOn(callback);
        }

        @Override // Binder interface
        public void onScreenTurnedOn() {
            Log.d(TAG, "mBinder: onScreenTurnedOn: ");
            checkPermission();
            mKeyguardViewMediator.onScreenTurnedOn();
        }

        @Override // Binder interface
        public void onScreenTurnedOff() {
            Log.d(TAG, "mBinder: onScreenTurnedOff: ");
            checkPermission();
            mKeyguardViewMediator.onScreenTurnedOff();
        }

        @Override // Binder interface
        public void setKeyguardEnabled(boolean enabled) {
            Log.d(TAG, "mBinder: setKeyguardEnabled: ");
            checkPermission();
            mKeyguardViewMediator.setKeyguardEnabled(enabled);
        }

        @Override // Binder interface
        public void onSystemReady() {
            Log.d(TAG, "mBinder: onSystemReady: ");
            checkPermission();
            mKeyguardViewMediator.onSystemReady();
        }

        @Override // Binder interface
        public void doKeyguardTimeout(Bundle options) {
            Log.d(TAG, "mBinder: doKeyguardTimeout: ");
            checkPermission();
            mKeyguardViewMediator.doKeyguardTimeout(options);
        }

        @Override // Binder interface
        public void setCurrentUser(int userId) {
            Log.d(TAG, "mBinder: setCurrentUser: ");
            checkPermission();
            mKeyguardViewMediator.setCurrentUser(userId);
        }

        @Override
        public void onBootCompleted() {
            Log.d(TAG, "mBinder: onBootCompleted: ");
            checkPermission();
            mKeyguardViewMediator.onBootCompleted();
        }

        @Override
        public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
            Log.d(TAG, "mBinder: startKeyguardExitAnimation: ");
            checkPermission();
            mKeyguardViewMediator.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }

        @Override
        public void onActivityDrawn() {
            Log.d(TAG, "mBinder: onActivityDrawn: ");
            checkPermission();
            mKeyguardViewMediator.onActivityDrawn();
        }
    };
}

