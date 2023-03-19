package com.example.CapstoneProjectCommunicationApp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.io.OutputStream;

@SuppressLint("all")
@SuppressWarnings("all")

public class BluetoothVariables {

    // Socket and stream global variables
    static public BluetoothSocket btSocket;
    static public InputStream inStream;
    static public OutputStream outStream;

    // Connected devices name global variable
    static public String connectedDeviceName;

    // Create thread stop condition for receive thread (Created globally so that
    // threads can access it even after an activity restart!)
    static public boolean stopReceiveThread;
    static public boolean stopContinuousThread;

    // Create the variable that will hold the received data (Created globally so that
    // the receive thread can access it even after an activity restart!)
    static public int bytes;

}
