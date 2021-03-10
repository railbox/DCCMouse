package com.dccmause.dccmause;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import static com.dccmause.dccmause.Interfaces.IType.BT_SPP;

class Interfaces {
    private static SPP mSPP;
    @SuppressLint("StaticFieldLeak")
    private static BLE mBLE;
    private static WiFi mWiFi;

    enum IType {
        BT_SPP, BT_BLE, WIFI
    }
    private static IType mType;

    private static BluetoothAdapter mBluetoothAdapter;

    static final int STATE_DISCONNECTED = 0;
    static final int STATE_CONNECTING = 1;
    static final int STATE_CONNECTED = 2;
    static final int STATE_SEARCHING = 3;
    static final int STATE_SERVICE_FOUNDED = 4;
    static final int STATE_SERVICE_DISCOVERED = 5;
    static final int STATE_NOT_FOUNDED = 6;
    static final int STATE_NOT_BONDED = 7;
    static final int STATE_ADAPTER_ERROR = 8;

    interface DataCallback {
        void onDataReceived(byte[] data);
    }
    interface ConnectionStatusCallback {
        void onStatusChange(final int status);
    }
    static DataCallback mDataCallback;
    static ConnectionStatusCallback mConnectionStatusCallback;

    Interfaces(Context context, IType type) {
        mType = type;
        // Initializes Interfaces adapter.
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        switch (mType) {
            case BT_BLE:
                mBLE = new BLE(context);
                break;
            case BT_SPP:
                mSPP = new SPP();
                break;
            case WIFI:
                mWiFi = new WiFi();
        }
    }

    void setDataReceivedHandler(DataCallback callback) {
        mDataCallback = callback;
    }
    void setConnectionStatusCallbackHandler(ConnectionStatusCallback callback) {
        mConnectionStatusCallback = callback;
    }

    boolean Connect(Context context){
        switch (mType) {
            case BT_BLE:
            case BT_SPP:
                if (mBluetoothAdapter != null) {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        ((Activity) context).startActivityForResult(enableIntent, 0);
                    }
                    if (mBluetoothAdapter.isEnabled()) {
                        if (mType == BT_SPP) mSPP.Connect(mBluetoothAdapter);
                        else mBLE.Connect(mBluetoothAdapter);
                        return true;
                    }
                }
                break;
            case WIFI:
                mWiFi.Connect();
                break;
        }
        mConnectionStatusCallback.onStatusChange(STATE_ADAPTER_ERROR);
        return false;
    }

    void Disconnect() {
        switch (mType) {
            case BT_SPP:
                mSPP.Disconnect();
                break;
            case BT_BLE:
                mBLE.Disconnect();
                break;
            case WIFI:
                mWiFi.Disconnect();
                break;
        }
    }

    boolean SendData(byte[] data) {
        switch (mType) {
            case BT_SPP:
                return mSPP.SendData(data);
            case BT_BLE:
                return mBLE.SendData(data);
            case WIFI:
                return mWiFi.SendData(data);
        }
        return false;
    }
}
