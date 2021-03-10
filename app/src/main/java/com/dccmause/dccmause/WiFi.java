package com.dccmause.dccmause;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

class WiFi {
    private static final String TAG = "WiFi";
    private static final String SERVER_IP = "192.168.0.100";
    private static final int SERVER_PORT = 5550;
    private Socket mSocket;
    private ConnectedThread connectedThread;

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
            UpdateStatus(Interfaces.STATE_CONNECTING);
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            try {
                InetAddress address = InetAddress.getByName(SERVER_IP);
                mSocket = new Socket(address, SERVER_PORT);

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
                connectedThread = new ConnectedThread(mSocket);
                connectedThread.start();
            } else {
                UpdateStatus(Interfaces.STATE_NOT_FOUNDED);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;

        ConnectedThread(Socket socket) {
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
                        if (Interfaces.mDataCallback != null) {
                            if ((size > 2) && (data[0] == (byte)0xFF) && (data[1] == (byte)0xFE))
                                Interfaces.mDataCallback.onDataReceived(Arrays.copyOfRange(data,2,data.length));
                            else Log.w(TAG, "Wrong packet received");
                        }
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


    void Connect() {
        new ConnectBT().execute();
    }

    void Disconnect() {
        if (mSocket != null){
            try {
                mSocket.close(); //close connection
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
            byte[] outputBuffer = new byte[data.length + 2];
            outputBuffer[0] = (byte)0xFF;
            outputBuffer[1] = (byte)0xFE;
            System.arraycopy(data,0,outputBuffer,2,data.length);
            try {
                mSocket.getOutputStream().write(outputBuffer);
            }catch (Exception e){
                Log.e(TAG,"SendData error",e);
                res = false;
            }
            if(res){
                String out = "Data sent: ";
                for (byte c : outputBuffer) {
                    out += Integer.toHexString(c) + " ";
                }
                Log.d(TAG, out);
                return true;
            }else {
                Log.w(TAG,"SendData: write error");
            }
        }else {
            Log.w(TAG,"SendData: Device not connected");
        }
        return false;
    }
}
