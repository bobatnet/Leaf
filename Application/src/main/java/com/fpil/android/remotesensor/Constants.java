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

/**
 * Defines several constants used between {@link BluetoothTalkService} and the UI.
 */
public interface Constants {

    // Message types sent from the BluetoothTalkService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_LEAF_NEW = 7;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int CONNECTION_LOST = 6;

    public static final byte CMD_NEW_MEASURE = (byte) 0x80;
    public static final byte CMD_OK = (byte) 0x81;

    public static final char RSP_ACK = 0x21;

    public static final byte MARKER_START = (byte) 0x5F;
    public static final byte MARKER_NEW_LEAF = (byte) 0x40;
    public static final byte MARKER_ALL_END = (byte) 0x50;
    public static final byte MARKER_END = (byte) 0x03;

    public static final int MESSAGE_SZ = 30;

    // Key names received from the BluetoothTalkService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

}
