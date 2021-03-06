/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

public class BluetoothControllerImpl implements BluetoothController, BluetoothCallback,
        CachedBluetoothDevice.Callback {
    private static final String TAG = "BluetoothController";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private final LocalBluetoothManager mLocalBluetoothManager;

    private boolean mEnabled;
    private int mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;
    private CachedBluetoothDevice mLastDevice;

    private final H mHandler = new H();

    public BluetoothControllerImpl(Context context, Looper bgLooper) {
        mLocalBluetoothManager = LocalBluetoothManager.getInstance(context, null);
        if (mLocalBluetoothManager != null) {
            mLocalBluetoothManager.getEventManager().setReceiverHandler(new Handler(bgLooper));
            mLocalBluetoothManager.getEventManager().registerCallback(this);
            onBluetoothStateChanged(
                    mLocalBluetoothManager.getBluetoothAdapter().getBluetoothState());
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Log.d(TAG, "dump: ");
        pw.println("BluetoothController state:");
        pw.print("  mLocalBluetoothManager="); pw.println(mLocalBluetoothManager);
        if (mLocalBluetoothManager == null) {
            return;
        }
        pw.print("  mEnabled="); pw.println(mEnabled);
        pw.print("  mConnectionState="); pw.println(stateToString(mConnectionState));
        pw.print("  mLastDevice="); pw.println(mLastDevice);
        pw.print("  mCallbacks.size="); pw.println(mCallbacks.size());
        pw.println("  Bluetooth Devices:");
        for (CachedBluetoothDevice device :
                mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()) {
            pw.println("    " + getDeviceString(device));
        }
    }

    private static String stateToString(int state) {
        Log.d(TAG, "stateToString: ");
        switch (state) {
            case BluetoothAdapter.STATE_CONNECTED:
                return "CONNECTED";
            case BluetoothAdapter.STATE_CONNECTING:
                return "CONNECTING";
            case BluetoothAdapter.STATE_DISCONNECTED:
                return "DISCONNECTED";
            case BluetoothAdapter.STATE_DISCONNECTING:
                return "DISCONNECTING";
        }
        return "UNKNOWN(" + state + ")";
    }

    private String getDeviceString(CachedBluetoothDevice device) {
        Log.d(TAG, "getDeviceString: ");
        return device.getName() + " " + device.getBondState() + " " + device.isConnected();
    }

    @Override
    public void addStateChangedCallback(Callback cb) {
        Log.d(TAG, "addStateChangedCallback: ");
        mCallbacks.add(cb);
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    @Override
    public void removeStateChangedCallback(Callback cb) {
        Log.d(TAG, "removeStateChangedCallback: ");
        mCallbacks.remove(cb);
    }

    @Override
    public boolean isBluetoothEnabled() {
        Log.d(TAG, "isBluetoothEnabled: ");
        return mEnabled;
    }

    @Override
    public boolean isBluetoothConnected() {
        Log.d(TAG, "isBluetoothConnected: ");
        return mConnectionState == BluetoothAdapter.STATE_CONNECTED;
    }

    @Override
    public boolean isBluetoothConnecting() {
        Log.d(TAG, "isBluetoothConnecting: ");
        return mConnectionState == BluetoothAdapter.STATE_CONNECTING;
    }

    @Override
    public void setBluetoothEnabled(boolean enabled) {
        Log.d(TAG, "setBluetoothEnabled: ");
        if (mLocalBluetoothManager != null) {
            mLocalBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(enabled);
        }
    }

    @Override
    public boolean isBluetoothSupported() {
        Log.d(TAG, "isBluetoothSupported: ");
        return mLocalBluetoothManager != null;
    }

    @Override
    public void connect(final CachedBluetoothDevice device) {
        Log.d(TAG, "connect: ");
        if (mLocalBluetoothManager == null || device == null) return;
        device.connect(true);
    }

    @Override
    public void disconnect(CachedBluetoothDevice device) {
        Log.d(TAG, "disconnect: ");
        if (mLocalBluetoothManager == null || device == null) return;
        device.disconnect();
    }

    @Override
    public String getLastDeviceName() {
        Log.d(TAG, "getLastDeviceName: ");
        return mLastDevice != null ? mLastDevice.getName() : null;
    }

    @Override
    public Collection<CachedBluetoothDevice> getDevices() {
        Log.d(TAG, "getDevices: ");
        return mLocalBluetoothManager != null
                ? mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()
                : null;
    }

    private void updateConnected() {
        Log.d(TAG, "updateConnected: ");
        // Make sure our connection state is up to date.
        int state = mLocalBluetoothManager.getBluetoothAdapter().getConnectionState();
        if (state != mConnectionState) {
            mConnectionState = state;
            mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
        }
        if (mLastDevice != null && mLastDevice.isConnected()) {
            // Our current device is still valid.
            return;
        }
        mLastDevice = null;
        for (CachedBluetoothDevice device : getDevices()) {
            if (device.isConnected()) {
                mLastDevice = device;
            }
        }
        if (mLastDevice == null && mConnectionState == BluetoothAdapter.STATE_CONNECTED) {
            // If somehow we think we are connected, but have no connected devices, we aren't
            // connected.
            mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;
            mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
        }
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        Log.d(TAG, "onBluetoothStateChanged: ");
        mEnabled = bluetoothState == BluetoothAdapter.STATE_ON;
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        Log.d(TAG, "onScanningStateChanged: ");
        // Don't care.
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        Log.d(TAG, "onDeviceAdded: ");
        cachedDevice.registerCallback(this);
        updateConnected();
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        Log.d(TAG, "onDeviceDeleted: ");
        updateConnected();
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        Log.d(TAG, "onDeviceBondStateChanged: ");
        updateConnected();
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }

    @Override
    public void onDeviceAttributesChanged() {
        updateConnected();
        Log.d(TAG, "onDeviceAttributesChanged: ");
        mHandler.sendEmptyMessage(H.MSG_PAIRED_DEVICES_CHANGED);
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        Log.d(TAG, "onConnectionStateChanged: ");
        mLastDevice = cachedDevice;
        updateConnected();
        mConnectionState = state;
        mHandler.sendEmptyMessage(H.MSG_STATE_CHANGED);
    }

    private final class H extends Handler {
        private static final int MSG_PAIRED_DEVICES_CHANGED = 1;
        private static final int MSG_STATE_CHANGED = 2;

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "H: handleMessage: msg.what = " + msg.what);
            switch (msg.what) {
                case MSG_PAIRED_DEVICES_CHANGED:
                    firePairedDevicesChanged();
                    break;
                case MSG_STATE_CHANGED:
                    fireStateChange();
                    break;
            }
        }

        private void firePairedDevicesChanged() {
            Log.d(TAG, "firePairedDevicesChanged: ");
            for (BluetoothController.Callback cb : mCallbacks) {
                cb.onBluetoothDevicesChanged();
            }
        }

        private void fireStateChange() {
            Log.d(TAG, "fireStateChange: ");
            for (BluetoothController.Callback cb : mCallbacks) {
                fireStateChange(cb);
            }
        }

        private void fireStateChange(BluetoothController.Callback cb) {
            Log.d(TAG, "fireStateChange(args): ");
            cb.onBluetoothStateChange(mEnabled);
        }
    }
}
