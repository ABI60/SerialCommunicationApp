/*package com.example.CapstoneProjectCommunicationApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.ConstantCallSite;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SendReceiveActivity extends AppCompatActivity {

    // Create class variables for gui
    private TextView logText;
    private Button clearLogsBtn;
    private Button sendBtn;
    private Button startBtn;
    private Button stopBtn;
    private EditText singleSendTextInput;
    private EditText continuousSendTextInput;
    private EditText delayTextInput;

    // Bluetooth IO variables
    private BluetoothSocket socket;
    private InputStream inStream;
    private OutputStream outStream;

    // Create activity handler variable
    private Handler sendRecieveHandler;

    // Create thread stop condition for receive thread
    private boolean stopRecieveThread;
    private boolean stopContinuousThread;

    // Create the variable that will hold the received data
    private int bytes;

    // Create named constants to carry a message
    private final int Message_Recieved_Call = 1;
    private final int Message_Sent_Call = 2;

    @SuppressLint({"ClickableViewAccessibility", "HandlerLeak"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_recieve);

        // Assign GUI variables' id
        logText = (TextView) findViewById(R.id.logText);
        clearLogsBtn = (Button)findViewById(R.id.clearLogsBtn);
        sendBtn = (Button)findViewById(R.id.sendBtn);
        startBtn = (Button)findViewById(R.id.startBtn);
        stopBtn = (Button)findViewById(R.id.stopBtn);
        singleSendTextInput = (EditText)findViewById(R.id.singleSendTextInput);
        continuousSendTextInput = (EditText)findViewById(R.id.continuousSendTextInput);
        delayTextInput = (EditText)findViewById(R.id.delayTextInput);

        //Try to get the input and output streams of the socket
        try{
            socket = MainActivity.btSocket;
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
        }
        catch (IOException e){
            // Send a message to the handler on the main activity
            MainActivity.mainHandler.obtainMessage(3, 7, -1, null).sendToTarget();
            // Close the activity
            finish();
        }

        // Set thread stop condition to false initially, on startup
        stopRecieveThread = false;
        // Create a thread that runs in the background
        new Thread(){
            public void run() {
                // Loop until stop condition is set
                while(stopRecieveThread == false){
                    // Try to read the data
                    try{
                        // Read the data(this function blocks until it receives something to read)
                        bytes = inStream.read();
                        // Send a message to the handler to inform data received
                        //*Handler is used because you need as lightweight methods as possible
                        // in this thread otherwise the inStream buffer get overrun and app
                        // crashes! Also a thread can't change activity ui reliably! *
                        sendRecieveHandler.obtainMessage(Message_Recieved_Call).sendToTarget();
                    }catch (IOException e){
                        // *Coming down here means the connection is lost!*

                        // Send a message to the handler on the main activity
                        MainActivity.mainHandler.obtainMessage(3, 6, -1, null).sendToTarget();
                        // Close the activity
                        finish();
                        // Stop the thread
                        break;
                    }
                }
            }
        }.start();

        // Create a send receive activity handler for updating gui elements etc.
        sendRecieveHandler = new Handler(){
            public void handleMessage(android.os.Message msg) {
                // If the message says receive data happened
                if(msg.what == Message_Recieved_Call){
                    // Inform the user
                    logText.append("\nData received: " + bytes);
                }
                else if(msg.what == Message_Sent_Call){
                    int sentData = (Integer) msg.obj;
                    // Inform the user
                    logText.append("\nData sent: " + sentData);
                }
            }
        };

        // Make stop button initially off
        stopBtn.setEnabled(false);

        // Get the launch intent
        Intent intentVariable = getIntent();
        // Get the connected device name from the intent
        String deviceName = intentVariable.getStringExtra("Device name");
        // Add the info to the log
        logText.append("\n***Connected to: " + deviceName + "***");

        // Change the logtext background color
        logText.setBackgroundColor(Color.parseColor("#C0C0C0"));
        // Make the logtext scrollable
        logText.setMovementMethod(new ScrollingMovementMethod());
        // Make it so touching the logtext will stop the parents from interfering with the scroll
        logText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                // Check if the the touched object is logtext
                if (v.getId() == R.id.logText) {
                    // Stop the parents from interfering with the scroll
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    // Check when the user releases the touch
                    if(event.getAction() == MotionEvent.ACTION_UP){
                        // Allow the parents to interfere
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                // Return when the user has consumed the event
                return false;
            }
        });
        // On click listener for the "clear logs" button
        clearLogsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the logtext and inform the user
                logText.setText("***Cleared logs!***");
            }
        });
        // On click listener for the "send" button
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a variable to hold the input
                int input;
                // Put parsing into try to detect if user presses button with no input
                try {
                    // Parse the text to an integer variable
                    input = Integer.parseInt(singleSendTextInput.getText().toString());
                }catch (NumberFormatException e){
                    // Inform the user
                    logText.append("\n***You have to enter an input!***");
                    // Exit the event
                    return;
                }
                // Check if the number is between 0-255
                if(input<0 || input>255){
                    // Inform the user
                    logText.append("\n***Input must be between 0-255!***");
                    // Exit the event
                    return;
                }
                // Send the data
                write(input);
                // Send a message to the handler to inform the user
                sendRecieveHandler.obtainMessage(Message_Sent_Call, input).sendToTarget();
            }
        });
        // On click listener for the "start" button
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a variable to hold the input
                int input;
                int delay;

                // Put parsing into try to detect if user presses button with no input
                try {
                    // Parse the text to an integer variable
                    input = Integer.parseInt(continuousSendTextInput.getText().toString());
                }catch (NumberFormatException e){
                    // Inform the user
                    logText.append("\n***You have to enter an input!***");
                    // Exit the event
                    return;
                }
                // Put parsing into try to detect if user presses button with no input
                try {
                    // Parse the text to an integer variable
                    delay = Integer.parseInt(delayTextInput.getText().toString());
                }catch (NumberFormatException e){
                    // Inform the user
                    logText.append("\n***You have to enter a delay!***");
                    // Exit the event
                    return;
                }
                // Check if the number is between 0-255
                if(input<0 || input>255){
                    // Inform the user
                    logText.append("\n***Input must be between 0-255!***");
                    // Exit the event
                    return;
                }
                // Check if the number is between 0-255
                if(delay<100 || delay>10000){
                    // Inform the user
                    logText.append("\n***Delay must be between 100-10000***");
                    // Exit the event
                    return;
                }
                // Set the thread stop condition to false
                stopContinuousThread = false;
                // Start the continuous data send thread
                continuousWrite(input, delay);

                // Make button enabled configurations
                sendBtn.setEnabled(false);
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                singleSendTextInput.setEnabled(false);
                continuousSendTextInput.setEnabled(false);
                delayTextInput.setEnabled(false);

                // Inform the user
                logText.append("\n***Continuous send started!***");
            }
        });
        // On click listener for the "stop" button
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the thread stop condition to true
                stopContinuousThread = true;

                // Make button enabled configurations
                sendBtn.setEnabled(true);
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                singleSendTextInput.setEnabled(true);
                continuousSendTextInput.setEnabled(true);
                delayTextInput.setEnabled(true);

                // Inform the user
                logText.append("\n***Continuous send stopped!***");
            }
        });
    }// End of the onCreate

    // Class method to send the data
    public void write(int input) {
        // Try to send the data
        try {
            outStream.write(input);
        } catch (IOException e) {
            // *Coming down here means the connection is lost!*

            // Send a message to the handler on the main activity
            MainActivity.mainHandler.obtainMessage(3, 6, -1, null).sendToTarget();
            // Close the activity
            finish();
        }
    }
    // Create a method that uses another thread to send data continuously
    public void continuousWrite(int input, int delay){
        // Put the passed variables into final variables since methods require
        final int finalInput = input;
        final int finalDelay = delay;
        // Create a new thread
        new Thread(){
            public void run() {
                // Loop until stop condition is set
                while(stopContinuousThread == false) {
                    // Wait for the delay
                    SystemClock.sleep(finalDelay);
                    // Try to send the data
                    try {
                        // Send the data
                        outStream.write(finalInput);
                        // Send a message to the handler to inform data sent
                        //*Threads can't change activity ui reliably!*
                        sendRecieveHandler.obtainMessage(Message_Sent_Call, finalInput).sendToTarget();
                    } catch (IOException e) {
                        // *Coming down here means the connection is lost!*

                        // Send a message to the handler on the main activity
                        MainActivity.mainHandler.obtainMessage(3, 6, -1, null).sendToTarget();
                        // Close the activity
                        finish();
                        // Stop the thread
                        break;
                    }
                }
            }
        }.start();
    }
    // When the activity is destroyed from the stack(about to be terminated by whatever means)
    @Override
    public void onPause() {
        super.onPause();
        // Set the thread stop conditions for every possible running thread
        stopRecieveThread = true;
        stopContinuousThread = true;
        try{
            socket.close();
        }catch (IOException e){}
    }
    // When the instance is saved
    @Override
    public void onSaveInstanceState(Bundle outState) {
    //Save your data to be restored here
    //Example : outState.putLong("time_state", time); , time is a long variable
        outState.putString("Saved Log", logText.getText().toString());
        super.onSaveInstanceState(outState);
    }

}// End of the send receive activity class!
*/



/*
 package com.example.CapstoneProjectCommunicationApp;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.media.Image;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    // Create class variables for gui
    private ImageView bluetoothImage;
    private TextView bluetoothStatusText;
    private TextView connectionStatusText;
    private Button bluetoothOnBtn;
    private Button bluetoothOffBtn;
    private Button resetAvailableDevicesBtn;
    private ListView availableDevicesListView;

    // Create class variables for bluetooth
    private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> pairedDevices;
    static public BluetoothSocket btSocket = null;

    //Create class variable for adapter
    private ArrayAdapter<String> btArrayAdapter;

    // Create a handler for main activity
    static public Handler mainHandler;

    // Create thread spam protection variable
    private boolean threadSpamProtection = false;

    // Create named constants to carry a message
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;

    // Create a random identifier
    private static final UUID btUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register "ACTION_STATE_CHANGED" to be sent to the main broadcast
        this.registerReceiver(mainBroadcastReciever, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Assign GUI variables' id
        bluetoothImage = (ImageView) findViewById(R.id.bluetoothImage);
        bluetoothStatusText = (TextView) findViewById(R.id.bluetoothStatusText);
        connectionStatusText = (TextView) findViewById(R.id.connectionStatusText);
        bluetoothOnBtn = (Button) findViewById(R.id.bluetoothOnBtn);
        bluetoothOffBtn = (Button) findViewById(R.id.bluetoothOffBtn);

        //Initialize list view
        resetAvailableDevicesBtn = (Button) findViewById(R.id.resetAvailableDevicesBtn);
        availableDevicesListView = (ListView) findViewById(R.id.availableDevicesListView);
        btArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        availableDevicesListView.setAdapter(btArrayAdapter);

        // Get a hold of the default bluetooth module
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the device doesn't support bluetooth
        if(btAdapter == null){
            // Change bluetooth image to be "unavailable"
            bluetoothImage.setImageResource(R.drawable.ic_action_unavailable);
            // Update the bluetooth status text
            bluetoothStatusText.setText("Device doesn't support bluetooth.");
        }
        // Check if the bluetooth is enabled from the start
        if(btAdapter.isEnabled()){
            // Change bluetooth image to be "btopen"
            bluetoothImage.setImageResource(R.drawable.ic_action_btopen);
            // Update the bluetooth status text
            bluetoothStatusText.setText("Bluetooth is on.");
        }
        // If the bluetooth is disabled from the start
        else{
            // Change bluetooth image to be "btclosed"
            bluetoothImage.setImageResource(R.drawable.ic_action_btclosed);
            // Update the bluetooth status text
            bluetoothStatusText.setText("Bluetooth is disabled.");
        }
        // Get paired devices on startup
        pairedDevices = btAdapter.getBondedDevices();
        // Show paired devices on startup for each device
        for(BluetoothDevice device : pairedDevices){
            btArrayAdapter.add(" Name: " + device.getName() + "\nAdress: " + device.getAddress());
        }

        // Create a main activity handler for updating gui elements etc.
        mainHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                // Check if message is about connection status
                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1) {
                        // Update the connection status text
                        connectionStatusText.setText("Bluetooth is not on!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Bluetooth is not on!", Toast.LENGTH_SHORT).show();
                    }
                    else if(msg.arg1 == 2){
                        // Update the connection status text
                        connectionStatusText.setText("Could not secure a connection!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Could not secure a connection!", Toast.LENGTH_SHORT).show();
                    }
                    else if(msg.arg1 == 3){
                        // Update the connection status text
                        connectionStatusText.setText("Connection secured...");
                    }
                    else if(msg.arg1 == 4){
                        // Update the connection status text
                        connectionStatusText.setText("Connection failed!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Connection failed!", Toast.LENGTH_SHORT).show();
                    }
                    else if(msg.arg1 == 5){
                        //
                        String name = (String)msg.obj;
                        // Update the connection status text
                        connectionStatusText.setText("Connected to device!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Connected to device!", Toast.LENGTH_SHORT).show();
                        // Create an intent to launch "SendReceiveActivity"
                        Intent sucsessfullConnection = new Intent(getApplicationContext(), SendReceiveActivity.class);
                        sucsessfullConnection.putExtra("Device name", name);
                        // Launch "SendReceiveActivity"
                        startActivity(sucsessfullConnection);
                    }
                    else if(msg.arg1 == 6){
                        // Update the connection status text
                        connectionStatusText.setText("Connection lost!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Connection lost!", Toast.LENGTH_SHORT).show();
                    }
                    else if(msg.arg1 == 7){
                        // Update the connection status text
                        connectionStatusText.setText("Something went wrong!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Something went wrong!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        // On click listener for the "Turn On Bluetooth" button
        bluetoothOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the device doesn't support bluetooth
                if(btAdapter == null){
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Device doesn't support bluetooth.", Toast.LENGTH_SHORT).show();
                    // Don't do anything and return
                    return;
                }
                // Check if bluetooth is not on
                if(!btAdapter.isEnabled()){
                    // Create the intent with the bluetooth enable request
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Turning on bluetooth...", Toast.LENGTH_SHORT).show();
                    // Star activity for result with the bluetooth enable request intent
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                // If the bluetooth is already on
                else{
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Bluetooth is already on!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // On click listener for the "Turn Off Bluetooth" button
        bluetoothOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the device doesn't support bluetooth
                if(btAdapter == null){
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Device doesn't support bluetooth.", Toast.LENGTH_SHORT).show();
                    // Don't do anything and return
                    return;
                }
                // Disable bluetooth
                btAdapter.disable();
            }
        });

        // On click listener for the "reset" button
        resetAvailableDevicesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the bluetooth is not on
                if(!btAdapter.isEnabled()){
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Make sure bluetooth is on!", Toast.LENGTH_SHORT).show();
                }
                // First clear the list
                btArrayAdapter.clear();
                // Get paired devices
                pairedDevices = btAdapter.getBondedDevices();
                // Show paired devices for each device
                for (BluetoothDevice device : pairedDevices) {
                    btArrayAdapter.add(" Name: " + device.getName() + "\nAdress: " + device.getAddress());
                }
            }
        });

        // On click listener for when you hit an item in the devices list view
        availableDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // If the thread in this method is already running, exit
                if(threadSpamProtection == true){return;}
                // Update the connection status text
                connectionStatusText.setText("Connecting to device...");

                // Get the pressed items text as string
                String deviceInfo = ((TextView) view).getText().toString();
                // Separate the device's name and adress(last 17 characters are the device address!)
                final String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);
                final String deviceName = deviceInfo.substring(7, deviceInfo.length() - 26);
                // Enable the thread spam protection
                threadSpamProtection = true;
                new Thread() {
                    public void run() {
                        // Check if the bluetooth is not on
                        if (!btAdapter.isEnabled()) {
                            // Send the connection status to the handler
                            mainHandler.obtainMessage(CONNECTING_STATUS, 1, -1, null).sendToTarget();
                            // Enable the thread spam protection
                            threadSpamProtection = false;
                            // Don't do anything and return
                            return;
                        }
                        // Get a BluetoothDevice object for the given Bluetooth hardware address
                        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
                        // Try to create a connection
                        try {
                            // Create secure outgoing connection with BT device using UUID
                            btSocket = device.createRfcommSocketToServiceRecord(btUUID);
                        } catch (IOException e) {
                            // Send the connection status to the handler
                            mainHandler.obtainMessage(CONNECTING_STATUS, 2, -1, null).sendToTarget();
                            // Enable the thread spam protection
                            threadSpamProtection = false;
                            // Don't do anything and return
                            return;
                        }
                        // Try to establish a connection
                        try {
                            // Send the connection status to the handler
                            mainHandler.obtainMessage(CONNECTING_STATUS, 3, -1, null).sendToTarget();
                            // Establish the connection
                            btSocket.connect();
                        } catch (IOException e) {
                            // If connection fails try to close the connection
                            try {
                                // Send the connection status to the handler
                                mainHandler.obtainMessage(CONNECTING_STATUS, 4, -1, null).sendToTarget();
                                // Close the connection
                                btSocket.close();
                                // Enable the thread spam protection
                                threadSpamProtection = false;
                                // Don't do anything and return
                                return;
                            } catch (IOException e2) {
                                // Send the connection status to the handler
                                mainHandler.obtainMessage(CONNECTING_STATUS, 4, -1, null).sendToTarget();
                                // Enable the thread spam protection
                                threadSpamProtection = false;
                                // Don't do anything and return
                                return;
                            }
                        }
                        // Send the connection status to the handler
                        mainHandler.obtainMessage(CONNECTING_STATUS, 5, -1, deviceName).sendToTarget();
                        // Disable the thread spam protection
                        threadSpamProtection = false;
                    }
                }.start();
            }
        });

    }// End of the onCreate!

    /// Result check for "startActivityForResult" function
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // Check if the request was "enable request"
        if(requestCode == REQUEST_ENABLE_BT){
            // Check if the enable was successful
            if(!(resultCode == RESULT_OK)){
                // Update the bluetooth status text
                bluetoothStatusText.setText("Failed to turn on bluetooth.");
                // Show a toast message
                Toast.makeText(getApplicationContext(), "Failed to turn on bluetooth!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // Create a main broadcast receiver
    final BroadcastReceiver mainBroadcastReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the action from intent that was used to call the broadcast
            String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                if(btAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF){
                    // Change bluetooth image to be "btclosed"
                    bluetoothImage.setImageResource(R.drawable.ic_action_btclosed);
                    // Update the bluetooth status text
                    bluetoothStatusText.setText("Bluetooth is disabled.");
                    // Update the connection status text
                    connectionStatusText.setText("Connection terminated!");
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Bluetooth is turned off.", Toast.LENGTH_SHORT).show();
                }
                if(btAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON){
                    // Change bluetooth image to be "btopen"
                    bluetoothImage.setImageResource(R.drawable.ic_action_btopen);
                    // Update the bluetooth status text
                    bluetoothStatusText.setText("Bluetooth is on.");
                    // Update the connection status text
                    connectionStatusText.setText("Select a device.");
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Bluetooth was successfully turned on!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

}// End of the main activity class!

 */