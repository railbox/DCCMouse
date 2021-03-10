package com.dccmause.dccmause;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import java.util.UUID;
import java.lang.String;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

class BLE {
    private final Context mContext;
    private boolean mScanning;
    private final Handler mHandler = new Handler();
    private static final String TAG = "BLE";
    private static final String NameForFind = "CardberryCC2650";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mTxCharacteristic;
    private int mConnectionState = Interfaces.STATE_DISCONNECTED;

    private final static UUID GattServUUID       = UUID.fromString("14839ac4-7d7e-415c-9a42-167340cf2339");
    private final static UUID RXCharacterUUID    = UUID.fromString("0734594a-a8e7-4b1a-a6b1-cd5243059a57");
    private final static UUID RXDescriptorUUID   = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final static UUID TXCharacterUUID    = UUID.fromString("8b00ace7-eb0b-49b0-bbe9-9aee0a26e1a3");

    BLE(Context context){
        mContext = context;
    }

    private void UpdateStatus(final int status) {
        mConnectionState = status;
        if (Interfaces.mConnectionStatusCallback != null) Interfaces.mConnectionStatusCallback.onStatusChange(mConnectionState);
    }

    void Connect(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
        scanLeDevice(true);
    }

    void Disconnect() {
        if (mBluetoothGatt != null)
            mBluetoothGatt.disconnect();
    }

    boolean SendData(byte[] data) {
        if ((mConnectionState == Interfaces.STATE_CONNECTED) && (mTxCharacteristic != null)){
            mTxCharacteristic.setValue(data);
            if (mBluetoothGatt.writeCharacteristic(mTxCharacteristic)) {
                String out = "Data sent: ";
                for (byte c : data) {
                    out += Integer.toHexString(c) + " ";
                }
                Log.d(TAG, out);
                return true;
            }else {
                Log.w(TAG,"SendData: writeCharacteristic error");
            }
        }else {
            Log.w(TAG,"SendData: Device not connected");
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning) {
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan( mLeScanCallback);
                        UpdateStatus(Interfaces.STATE_NOT_FOUNDED);
                        Log.i(TAG, "Interfaces: Scan finished");
                    }
                }
            }, 10000);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            UpdateStatus(Interfaces.STATE_SEARCHING);
            Log.i(TAG, "Interfaces: Scan started");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            UpdateStatus(Interfaces.STATE_DISCONNECTED);
            Log.i(TAG, "Interfaces: Scan finished");
        }
    }


    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String devName = device.getName();
                    Log.i(TAG, "LeScanCallback Name: "+ devName);
                    if (devName.equals(NameForFind)) {
                        scanLeDevice(false);
                        UpdateStatus(Interfaces.STATE_CONNECTING);
                        Log.i(TAG, "LeScanCallback DeviceFounded, Connecting...");
                        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
                    }
                }
            };

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                UpdateStatus(Interfaces.STATE_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                UpdateStatus(Interfaces.STATE_DISCONNECTED);
                Log.i(TAG, "Disconnected from GATT server.");
            }else {
                Log.w(TAG, "OnConnectionStateChange Status=" +status+" State="+newState);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService mGattService;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                UpdateStatus(Interfaces.STATE_SERVICE_DISCOVERED);
                Log.i(TAG, "onServicesDiscovered Success");
                dumpServices(mBluetoothGatt);
                mGattService = mBluetoothGatt.getService(GattServUUID);
                if (mGattService != null) {
                    UpdateStatus(Interfaces.STATE_SERVICE_FOUNDED);
                    Log.i(TAG, "GattService founded");
                    mTxCharacteristic = mGattService.getCharacteristic(TXCharacterUUID);
                    if (mTxCharacteristic != null){
                        Log.i(TAG, "TxCharacteristic founded");
                    }
                    else {
                        Log.w(TAG, "TxCharacteristic not founded");
                    }
                    BluetoothGattCharacteristic mRxCharacteristic;
                    mRxCharacteristic = mGattService.getCharacteristic(RXCharacterUUID);
                    if (mRxCharacteristic != null){
                        Log.i(TAG, "RxCharacteristic founded");
                        mBluetoothGatt.setCharacteristicNotification(mRxCharacteristic, true);
                        BluetoothGattDescriptor mRxDescriptor = mRxCharacteristic.getDescriptor(RXDescriptorUUID);
                        mRxDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(mRxDescriptor);

                    }else {
                        Log.w(TAG, "RxCharacteristic not founded");
                    }
                }else {
                    Log.w(TAG, "GattService not founded");
                }

            }else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            //if (status == BluetoothGatt.GATT_SUCCESS) {
            //}
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged");
            byte[] data = characteristic.getValue();
            String out = "Data received: ";
            for(byte c : data) {
                out += Integer.toHexString(c)+" ";
            }
            Log.d(TAG,out);
            if (Interfaces.mDataCallback != null)
                Interfaces.mDataCallback.onDataReceived(data);
        }
    };

    private void dumpServices( BluetoothGatt gatt )
    {
        for( BluetoothGattService svc : gatt.getServices() )
        {
            String svc_uuid = svc.getUuid().toString();
            Log.i(TAG, "SERVICE ( " + svc_uuid + " )" );

            for( BluetoothGattCharacteristic chara : svc.getCharacteristics() )
            {
                String chr_uuid = chara.getUuid().toString();

                Log.i(TAG, "  CHAR ( " + chr_uuid + " ) ");

                for( BluetoothGattDescriptor desc : chara.getDescriptors() )
                {
                    Log.i(TAG, "    DESC: " + desc.getUuid() );
                }
            }
        }
    }
}
