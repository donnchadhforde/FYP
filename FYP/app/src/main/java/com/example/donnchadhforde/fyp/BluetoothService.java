package com.example.donnchadhforde.fyp;

import android.app.Application;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BluetoothService {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String TAG = "BTservice";
    private Handler mHandler;
    private BluetoothDevice mDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;


    public BluetoothService(BluetoothDevice device, Handler handler) {
        this.mDevice = device;
        this.mHandler = handler;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mState = STATE_NONE;
    }


    public BluetoothDevice getDevice() {
        return this.mDevice;
    }



    public int getState() {
        return this.mState;
    }

    public synchronized void connect(BluetoothDevice device) {

        Log.d(TAG, "Connecting to: " + device.getName());

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device){

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

    }

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }


    private class ConnectThread extends Thread {

        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {

            BluetoothSocket tmp = null;
            this.mDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d(TAG, "Created rfcomm");
            } catch(IOException e) {
                Log.e(TAG, "Socket's create method failed");
            }
            mSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.d(TAG, "in run method of ConnectThread");
            mBluetoothAdapter.cancelDiscovery();

            try{
                Log.d(TAG, "Attempting connection...");
                mSocket.connect();
            } catch(IOException e) {
                Log.d(TAG, e.getMessage());
                try{
                    mSocket.close();
                    Log.d(TAG, "Connection failed");
                } catch(IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }

                return;
            }
            Log.d(TAG, "Connected");

            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            connected(mSocket, mDevice);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch(IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }

    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;
        private byte[] mBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            this.mSocket = socket;
            InputStream tmpin = null;
            OutputStream  tmpout = null;

            try{
                tmpin = socket.getInputStream();
            } catch(IOException e) {
                Log.e(TAG, "Error occured when creating input stream", e);
            }

            try{
                tmpout = socket.getOutputStream();
            } catch(IOException e) {
                Log.e(TAG, "Error occured when creating output stream", e);
            }

            mInStream = tmpin;
            mOutStream = tmpout;
            mState = STATE_CONNECTED;
        }

        public void run() {
            mBuffer = new byte[2048];
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream.
                    bytes = mInStream.read(mBuffer);

                    //Log.d(TAG, mBuffer.toString());
                    // Send the obtained bytes to the UI activity.
                    String readMessage = new String(mBuffer, 0, bytes);
                    Log.d(TAG, "Received: " + readMessage);
                    if (readMessage.substring(0, 2).equals("!-")) {
                        buildQuaternion(readMessage);
                    }
                    Message readMsg = mHandler.obtainMessage(
                            Constants.MESSAGE_READ, bytes, -1, mBuffer);

                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mOutStream.write(bytes);

                // Share the sent message with the UI activity.
//                Message writtenMsg = mHandler.obtainMessage(
//                        MessageConstants.MESSAGE_WRITE, -1, -1, mBuffer);
//                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
//                Message writeErrorMsg =
//                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
//                writeErrorMsg.setData(bundle);
//                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    public void buildQuaternion(String msg) {
        String[] quats = msg.split(",");
        String first = quats[0].replaceAll("!-", "");
        String last = quats[3].replaceAll("-!", "");
        float qw = Float.valueOf(first);
        float qx = Float.valueOf(quats[1]);
        float qy = Float.valueOf(quats[2]);
        float qz = Float.valueOf(last);

        OpenGLRenderer.quat = new Quaternion(qw, qx, qy, qz);

    }

}
