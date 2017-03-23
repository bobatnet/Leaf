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

package com.fpil.android.remotesensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.fpil.android.common.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothTalkService {
    // Debugging
    private static final String TAG = "BluetoothTalkService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    Context context;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param con The UI Activity Context     *
     */
    public BluetoothTalkService(Context con) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        context = con;
    }

    public void setHandler(Handler h) {
        mHandler = h;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
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

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothTalkService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
//        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(Constants.TOAST, "Device connection was lost");
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);

        Message msg = mHandler.obtainMessage(Constants.CONNECTION_LOST);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothTalkService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothTalkService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */

    private final UUID UUID_STRING_WELL_KNOWN_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            UUID_STRING_WELL_KNOWN_SPP);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            UUID_STRING_WELL_KNOWN_SPP);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothTalkService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    enum DSTATE {
        TAKING_OBS,WAITING
    }
    DSTATE dstate;

    public ArrayList<Integer> inRedArray, inGreenArray, inBlueArray, inCArray, inLeafNumber;
    public int incomingIndex = 0, leafIndex = 0;

    public void clearObservations(){
        inRedArray = new ArrayList<>(); inGreenArray = new ArrayList<>(); inBlueArray = new ArrayList<>(); inCArray = new ArrayList<>(); inLeafNumber = new ArrayList<>();
        incomingIndex = 0; leafIndex = 0;
    }

    protected class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            dstate = DSTATE.WAITING;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, String.format("maxReceive = %d", mmSocket.getMaxReceivePacketSize()));
            }
        }

        public void run_() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[2048];
            int bytes = 0;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    bytes = mmInStream.read(buffer, 0, 100);

                    String str = "";
                    for (byte aMsg : buffer) str += String.format(" %x", (byte) aMsg);
                    Log.d(TAG, str);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();

//                    dstate = DSTATE.WAITING;
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothTalkService.this.start();
                    break;
                }
            }
        }

        public void run2() {
            Log.i(TAG, "BEGIN mConnectedThread");
            int readPosn = 0;
            byte[] buffer = new byte[2048];
            int bytes;
            final String LogTag = "BTRun";
            int r, g, b, c;
            clearObservations();

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    Log.d(LogTag, String.format("Read position %d", readPosn));
                    bytes = mmInStream.read(buffer, readPosn, 2048 - readPosn);

                    String str = "";
                    for (byte aMsg : buffer) str += String.format(" %x", (byte) aMsg);
                    Log.d(TAG, str);

                    int f = 0, z = 0, prevReadPos = 0;
                    int newLeaf;
                    int endLeaf;
                    byte chk;
                    byte[] msg = buffer;

                    try {
                        for (int x = 0; x < readPosn + bytes; x++) {
                            newLeaf = 0; endLeaf = 0;
                            while ((msg[x] == 0x5f) || (f == 0)) {
                                x++;
                                if (msg[x] > 0)
                                    Log.d(LogTag, String.format("%x ", msg[x]));
                                if (msg[x] == Constants.MARKER_NEW_LEAF) {
                                    Log.d(LogTag, ">>> New leaf");
                                    newLeaf++;
                                }
                                if (msg[x] == 0x5f) f = 1;
//                                if ((endLeaf == 0) && (msg[x] == 0xff)) endLeaf++;
//                                if ((endLeaf == 1) && (msg[x] == 0x00)) endLeaf++;
//                                if ((endLeaf == 2) && (msg[x] == 0x00)) endLeaf++;
//                                if ((endLeaf == 3) && (msg[x] == 0xff)) endLeaf++;
                            }
                            x -= 1;
                            chk = 0;
                            r = (msg[x + 1] * 256) + msg[x + 2]; g = (msg[x + 3] * 256) + msg[x + 4]; b = (msg[x + 5] * 256) + msg[x + 6]; c = (msg[x + 7] * 256) + msg[x + 8];
                            for (int y = 0; y < 4 * 2 + 2; y++) chk ^= msg[y + x];

                            x += 10;
                            Log.d(LogTag, String.format("%x %x %x %x", msg[x], msg[x+1], msg[x+2], msg[x+3]));

//                            if (newLeaf > 1) leafIndex++;
                            if (endLeaf == 4) leafIndex++;

                            if ((msg[x] == Constants.MARKER_END) && (chk == 0)) {
                                prevReadPos = x+1;
                                try {
                                    z += 1;
                                    inRedArray.set(incomingIndex, r); inGreenArray.set(incomingIndex, g); inBlueArray.set(incomingIndex, b); inCArray.set(incomingIndex, c); inLeafNumber.set(incomingIndex, leafIndex);
                                } catch (IndexOutOfBoundsException e) {
                                    inRedArray.add(r); inGreenArray.add(g); inBlueArray.add(b); inCArray.add(c); inLeafNumber.add(leafIndex);
                                }
                                incomingIndex++;
                                Log.d(LogTag, String.format("R = %d, G = %d, B = %d, C = %d, chk = %d, e = %d, leaf = %d, in = %d", r, g, b, c, chk, msg[x], leafIndex, incomingIndex));
                            }
                            f = 0;
                            x += 1;
                            if ((msg[x] == 0xff) && (msg[x+1] == 0x00) && (msg[x+2] == 0x00) && (msg[x] == 0xff)) leafIndex++;
                            x += 4;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.d(LogTag, "No more data");
                    }
                    if (z > 0) {
//                        showToast(String.format("%d data points", z), Toast.LENGTH_SHORT);
                    }

//                    Log.d(LogTag, String.format("Position %d", prevReadPos));
//                    for (int i = prevReadPos; !(msg[i] == 0 && msg[i+1] == 0); i++) {
//                        buffer[i-prevReadPos] = buffer[i];
//                        readPosn = i+1;
//                    }

                    // Send the obtained bytes to the UI Activity
//                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothTalkService.this.start();
                    break;
                }
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            int readPosn = 0;
            byte[] buffer = new byte[2048];
            int bytes;
            final String LogTag = "BTRun";
            int r, g, b, c;
            byte chk;
            clearObservations();
            Date now;
            long lastd = 0;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    do {
                        bytes = mmInStream.read(buffer, readPosn, 1);
                    } while ((buffer[readPosn] == 0x0A) || (buffer[readPosn] == 0x0D));

                    bytes = mmInStream.read(buffer, readPosn, 128 - readPosn);
                    String str = "";
                    for (byte aMsg : buffer) str += String.format(" %x", (byte) aMsg);
                    Log.d(TAG, String.format("Received %d bytes", bytes));
                    byte[] msg = buffer;
                    boolean newLeaf = true;
                    long inIndex = incomingIndex;

                    try {
                        for (int x = 0; x < bytes; x++) {
                            while (!((msg[x] == 0x5f) && (msg[x + 1] == 0x5f) && (msg[x + 2] == 0x5f))) {
                                x++;
                            }
                            x += 2;
                            chk = 0x5f;
                            r = (msg[x + 1] * 256) + msg[x + 2];
                            g = (msg[x + 3] * 256) + msg[x + 4];
                            b = (msg[x + 5] * 256) + msg[x + 6];
                            c = (msg[x + 7] * 256) + msg[x + 8];

                            int a = g;
                            g = r;
                            r = a;

                            for (int y = 1; y < 4 * 2 + 2; y++) chk ^= msg[y + x];

                            x += 10;

                            if (((msg[x] == Constants.MARKER_END) && (chk == 0)) && (!newLeaf)){
                                try {
                                    inRedArray.set(incomingIndex, r);
                                    inGreenArray.set(incomingIndex, g);
                                    inBlueArray.set(incomingIndex, b);
                                    inCArray.set(incomingIndex, c);
                                    inLeafNumber.set(incomingIndex, leafIndex);
                                } catch (IndexOutOfBoundsException e) {
                                    inRedArray.add(r);
                                    inGreenArray.add(g);
                                    inBlueArray.add(b);
                                    inCArray.add(c);
                                    inLeafNumber.add(leafIndex);
                                }
                                incomingIndex++;
                                Log.d(LogTag, String.format("R = %d, G = %d, B = %d, C = %d, chk = %d, e = %d, leaf = %d, in = %d", r, g, b, c, chk, msg[x], leafIndex, incomingIndex));
                            }
                            newLeaf = false;

                            now = new Date();
                            if ((now.getTime() - lastd > 1000)) // ||
                                    //((msg[x + 1] == -1) && (msg[x + 2] == 0) && (msg[x + 3] == 0) && (msg[x + 4] == -1)))
                            {
                                leafIndex++;
                                Log.d(LogTag, "New Leaf");
                                newLeaf = true;
                                mHandler.obtainMessage(Constants.MESSAGE_LEAF_NEW, leafIndex, incomingIndex).sendToTarget();
                            }
                            lastd = now.getTime();

                            if ((msg[x + 1] | msg[x + 2] | msg[x + 3] | msg[x + 4]) == 0) break;
                        }
//                        if (inIndex != incomingIndex) Toast.makeText(context, "Data captured", Toast.LENGTH_SHORT).show();
                    } catch (ArrayIndexOutOfBoundsException e) {}
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);

                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothTalkService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}