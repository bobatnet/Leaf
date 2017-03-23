package com.fpil.android.remotesensor;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.fpil.android.common.logger.Log;

import java.util.ArrayList;

/**
 * Created by bob on 15/11/16.
 */

public class Talker extends Handler {
    final String LogTag = "Talker";
    BluetoothTalkService chatter;
    Context context;

    public Talker(MainDisplay bigAct, BluetoothTalkService chat) {
        super();
        context = bigAct.getContext();
        chatter = chat;
        myState = STATE.IDLE;
        incomingIndex = 0;
        inRedArray = new ArrayList<>(100);
        inGreenArray = new ArrayList<>(100);
        inBlueArray = new ArrayList<>(100);
        inCArray = new ArrayList<>(100);
        inLeafNumber = new ArrayList<>(100);
    }
    private String mConnectedDeviceName;

    enum STATE {
        TAKING_MEAS, IDLE
    }
    private STATE myState;

    private int incomingIndex, leafIndex;

    private ArrayList<Integer> inRedArray, inGreenArray, inBlueArray, inCArray, inLeafNumber;

    private void clearData() { incomingIndex = 0; leafIndex = 0; }
    
    public void newMeasure() {
        clearData();
        byte[] out = new byte[1];
        out[0] = Constants.CMD_NEW_MEASURE;
        chatter.write(out);
        myState = STATE.TAKING_MEAS;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_STATE_CHANGE:
                myState = STATE.IDLE;

                switch (msg.arg1) {
                    case BluetoothTalkService.STATE_CONNECTED:
                        setActivityStatus(R.string.title_connected);
//                            mConversationArrayAdapter.clear();
                        break;
                    case BluetoothTalkService.STATE_CONNECTING:
                        setActivityStatus(R.string.title_connecting);
                        break;
                    case BluetoothTalkService.STATE_LISTEN:
                    case BluetoothTalkService.STATE_NONE:
                        setActivityStatus(R.string.title_not_connected);
                        break;
                }
                break;
            case Constants.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
//                String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case Constants.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                break;
//                if (readBuf[0] == Constants.RSP_TYPE_ONLY_GREEN) {
//                    if (readBuf[3] == Constants.RSP_END){
//                        int chk = readBuf[0] ^ readBuf[1] ^ readBuf[2] ^ readBuf[3] ^ readBuf[4];
//                        if (chk == 0) {
//                            long v = ((int) readBuf[1]) + (((int) readBuf[2]) << 8);
//                            try {
//                                measures.add(v);
//                            } catch (NullPointerException e){
//                                Toast.makeText(activity.getActivity(), R.string.measurement_not_started, Toast.LENGTH_LONG);
//                            }
//                            byte[] out = new byte[1];
//                            out[0] = Constants.CMD_OK;
//                            chatter.write(out);
//                        }
//                    }
//                }
//                String readMessage = new String(readBuf, 0);

//                readInFull(readBuf);

//                if (myState == STATE.TAKING_MEAS) {
//                    if ((incomingIndex == 100) || (readBuf[0] == Constants.MARKER_ALL_END)){
//                        flushData();
//                        myState = STATE.IDLE;
//                    } else {
//                        readInBytes(readBuf);
//                    }
//                }
            case Constants.MESSAGE_LEAF_NEW:
                setActivityStatus(String.format("Leaf %d", msg.arg1));
                showToast(String.format("New Leaf %d", msg.arg1), Toast.LENGTH_SHORT);
                break;
            case Constants.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                showToast("Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT);
                break;
            case Constants.MESSAGE_TOAST:
                showToast(msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT);
                break;
            case Constants.CONNECTION_LOST:
                showToast("Connection lost!", Toast.LENGTH_LONG);
                myState = STATE.IDLE;
        }
    }

    ArrayList<Byte> unread;

    private boolean readInFull(final byte[] msg) {
//        ArrayList<Byte> msg = new ArrayList<>(unread);
//        msg.addAll(msgBytes);

        int f = 0, x = 0, r, g, b, c, z = 0, prevReadPos = 0;
        int newLeaf = 0;
        byte chk;

        try {
            for (int i = 0; i < Constants.MESSAGE_SZ; i++) {
                newLeaf = 0;
                while ((msg[x] == 0x5f) || (f == 0)) {
                    if (msg[x] > 0)
                        Log.d(LogTag, String.format(">>> %x", msg[x]));
                    x++;
                    if (msg[x] == Constants.MARKER_NEW_LEAF) {
                        Log.d(LogTag, ">>> New leaf");
                        newLeaf++;
                    }

                    if (msg[x] == 0x5f) f = 1;
                }
                x -= 1;
                chk = 0;
                r = (msg[x + 1] * 256) + msg[x + 2];
                g = (msg[x + 3] * 256) + msg[x + 4];
                b = (msg[x + 5] * 256) + msg[x + 6];
                c = (msg[x + 7] * 256) + msg[x + 8];
                for (int y = 0; y < 4 * 2 + 2; y++) {
                    chk ^= msg[y + x];
                }
                x += 10;
                if (newLeaf >= 1) leafIndex++;

                if ((msg[x] == Constants.MARKER_END) && (chk == 0)) {
                    prevReadPos = x;
                    try {
                        z += 1;
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
//                    Log.d(LogTag, "Incoming OK");
                    Log.d(LogTag, String.format("R = %d, G = %d, B = %d, C = %d, chk = %d, e = %d, leaf = %d, in = %d", r, g, b, c, chk, msg[x], leafIndex, incomingIndex));
                }
                f = 0;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.d(LogTag, "No more data");
        }
        if (z > 0) {
            showToast(String.format("%d data points", z), Toast.LENGTH_SHORT);
//            leafIndex++;
        }

        unread = new ArrayList<>();
        for (int i = prevReadPos; msg[i] != 0; i++) {
            unread.add(msg[i]);
        }
        return true;
    }

    private boolean readInBytes(final byte[] msg){
        int i;
        String str = "";
        for (byte aMsg : msg) str += String.format(" %x", (byte) aMsg);
        Log.d(LogTag, str);

        if (msg[0] == Constants.MARKER_START) {
            i = 0;
            int x = 0;
            int r=-1,g=-1,b=-1,c=-1;

            try {
                r = (msg[i+1] * 256) + msg[i+2];
                g = (msg[i+3] * 256) + msg[i+4];
                b = (msg[i+5] * 256) + msg[i+6];
                c = (msg[i+7] * 256) + msg[i+8];

                for (int j = 0; j < 10; j++)
                    x ^= msg[j];
                x = (x % 256) ^ (x / 256);
            } catch (StringIndexOutOfBoundsException e) {
                Log.d(LogTag, "Data error");
                x = 1;
            }

            if (x > 1) {  // skip checksum
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
                Log.d(LogTag, "Incoming OK");
                Log.d(LogTag, String.format("incIndex = %d", incomingIndex));
            } else {
                if (x > 1)
                    Log.d(LogTag, String.format("Checksum: %x", x));
            }
            return true;
        }
        byte[] out = new byte[1];
        if (msg[0] == Constants.MARKER_NEW_LEAF) {
            leafIndex++;
            out[0] = Constants.CMD_OK;
            chatter.write(out);
            Log.d(LogTag, "New leaf");
            return true;
        }
        if (msg[0] == Constants.MARKER_ALL_END) {
            out[0] = Constants.CMD_OK;
            chatter.write(out);
            flushData();
            return true;
        }
        return false;
    }

    public void flushData() {
        showToast("Received entire data.", Toast.LENGTH_LONG);
        Log.d(LogTag, String.format("incomingIndex %d, leafIndex %d",incomingIndex,leafIndex));

        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(context);

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        long newRowId = -1;
        // Create a new map of values, where column names are the keys
        for (int i=0; i < incomingIndex; i++) {
            ContentValues row = new ContentValues();

            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_Red, inRedArray.get(i));
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_Green, inGreenArray.get(i));
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_Blue, inBlueArray.get(i));
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_C, inCArray.get(i));
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_Key, this.getCurrentKey());
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_DateTime, this.getCurrentKey());
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_LeafIndex, inLeafNumber.get(i));

            // Insert the new row, returning the primary key value of the new row
            newRowId = db.insert(FeedReaderDbHelper.Table_DB_Raw_Data, null, row);
        }
        Log.d(LogTag, String.format("Saved raw data %d.", newRowId));
        clearData();
    }

    public void flushData(ArrayList<Integer> inRedArray, ArrayList<Integer> inGreenArray, ArrayList<Integer> inBlueArray, ArrayList<Integer> inCArray, ArrayList<Integer> inLeafNumber,
                          int incomingIndex, int leafIndex) {
        showToast("Received entire data.", Toast.LENGTH_LONG);
        Log.d(LogTag, String.format("incomingIndex %d, leafIndex %d",incomingIndex,leafIndex));

        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(context);

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        long newRowId = -1;
        // Create a new map of values, where column names are the keys
        for (int i=0; i < incomingIndex; i++) {
            ContentValues row = new ContentValues();

            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_Red, inRedArray.get(i));
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_Green, inGreenArray.get(i));
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_Blue, inBlueArray.get(i));
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_C, inCArray.get(i));
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_Key, this.getCurrentKey());
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_DateTime, this.getCurrentKey());
            row.put(FeedReaderDbHelper.Col_DB_Raw_Data_LeafIndex, inLeafNumber.get(i));

            // Insert the new row, returning the primary key value of the new row
            newRowId = db.insert(FeedReaderDbHelper.Table_DB_Raw_Data, null, row);
        }
        Log.d(LogTag, String.format("Saved raw data %d.", newRowId));
        clearData();
    }

    void showToast(CharSequence text, int duration) {}
    void setActivityStatus(int resId) {}
    void setActivityStatus(String s) {}
    long getCurrentKey() { return 0; }
}
