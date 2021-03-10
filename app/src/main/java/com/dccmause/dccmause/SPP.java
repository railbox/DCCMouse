package com.dccmause.dccmause;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.lang.String;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

class SPP {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket btSocket;
    private String ConnectAddress = null;
    private ConnectedThread connectedThread;
    private static final String TAG = "BT_SPP";
    private static final String NameForFind = "XpressNet";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private int mConnectionState = Interfaces.STATE_DISCONNECTED;

    private void UpdateStatus(final int status) {
        mConnectionState = status;
        if (Interfaces.mConnectionStatusCallback != null) Interfaces.mConnectionStatusCallback.onStatusChange(mConnectionState);
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            //progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");
            UpdateStatus(Interfaces.STATE_CONNECTING);
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            try {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(ConnectAddress);
                btSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                btSocket.connect();
            } catch (Exception e) {
                ConnectSuccess = false;
                Log.w(TAG,"Connect error: "+e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
            if (ConnectSuccess) {
                UpdateStatus(Interfaces.STATE_CONNECTED);
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();

            } else {
                UpdateStatus(Interfaces.STATE_NOT_FOUNDED);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;

        ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Temp sockets not created", e);
            }
            mmInStream = tmpIn;
        }

        public void run() {
            Log.i(TAG, "Begin connectedThread");
            byte[] buffer = new byte[256];  // buffer store for the stream
            int size; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    size = mmInStream.read(buffer);
                    if (size>0) {
                        byte[] data = Arrays.copyOf(buffer,size);
                        String out = "Data received: ";
                        for (byte c : data) {
                            out += Integer.toHexString(c) + " ";
                        }
                        Log.d(TAG, out);
                        if (Interfaces.mDataCallback != null)
                            Interfaces.mDataCallback.onDataReceived(data);
                    }

                } catch (IOException e) {
                    Log.w(TAG, "Connection Lost: "+e.getMessage());
                    UpdateStatus(Interfaces.STATE_DISCONNECTED);
                    connectedThread = null;
                    break;
                }
            }
        }
    }

    void Connect(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                if (bt.getName().equals(NameForFind)){
                    ConnectAddress = bt.getAddress();
                    break;
                }
            }
        }
        if (ConnectAddress != null)
            new ConnectBT().execute();
        else UpdateStatus(Interfaces.STATE_NOT_BONDED);
    }

    void Disconnect() {
        if (btSocket != null){
            try {
                btSocket.close(); //close connection
                UpdateStatus(Interfaces.STATE_DISCONNECTED);
                connectedThread = null;
            } catch (Exception e) {
                Log.e(TAG,"Disconnect error",e);
            }
        }
    }

    boolean SendData(byte[] data) {
        if (mConnectionState == Interfaces.STATE_CONNECTED){
            boolean res = true;
            try {
                btSocket.getOutputStream().write(data);
            }catch (Exception e){
                Log.e(TAG,"SendData error",e);
                res = false;
            }
            if(res){
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
}
