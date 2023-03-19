package com.example.CapstoneProjectCommunicationApp;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@SuppressLint("all")
@SuppressWarnings("all")

public class MainActivity extends AppCompatActivity {

    //region -Create class variables
    // Gui variables
    private ImageView bluetoothImage;
    private TextView bluetoothStatusText;
    private TextView connectionStatusText;
    private Button bluetoothOnBtn;
    private Button bluetoothOffBtn;
    private Button resetAvailableDevicesBtn;
    private ListView availableDevicesListView;

    // Create bluetooth adapter variable to hold default bluetooth module data
    private BluetoothAdapter btAdapter;

    // Create set of bluetooth device variable to hold paired devices data
    private Set<BluetoothDevice> pairedDevices;

    // Create array adapter variable for the pared devices list
    private ArrayAdapter<String> btArrayAdapter;

    // Create a handler for main activity(static to make it accessible to other activities)
    static public Handler mainHandler;

    // Create thread spam protection variable
    private boolean threadSpamProtection = false;

    // Create named constants to carry a message
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;

    // Create a random identifier to secure a connection to socket
    private static final UUID btUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //endregion

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region -Assign GUI variables
        bluetoothImage = (ImageView) findViewById(R.id.bluetoothImage);
        bluetoothStatusText = (TextView) findViewById(R.id.bluetoothStatusText);
        connectionStatusText = (TextView) findViewById(R.id.connectionStatusText);
        bluetoothOnBtn = (Button) findViewById(R.id.bluetoothOnBtn);
        bluetoothOffBtn = (Button) findViewById(R.id.bluetoothOffBtn);
        resetAvailableDevicesBtn = (Button) findViewById(R.id.resetAvailableDevicesBtn);

        // Initialize list view and assign the set and a default layout to it
        availableDevicesListView = (ListView) findViewById(R.id.availableDevicesListView);
        btArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        availableDevicesListView.setAdapter(btArrayAdapter);
        //endregion

        // Get a hold of the default bluetooth module
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the device doesn't support bluetooth
        if(btAdapter == null){
            // Change bluetooth image to be "unavailable"
            bluetoothImage.setImageResource(R.drawable.ic_action_unavailable);
            // Update the bluetooth status text
            bluetoothStatusText.setText("Device doesn't support bluetooth!");
            // Update connection status text
            connectionStatusText.setText("Connection not possible!");
            // Don't initiate anything and return
            return;
        }
        // Check if the bluetooth is enabled from the start
        if(btAdapter.isEnabled() == true){
            // Change bluetooth image to be "btopen"
            bluetoothImage.setImageResource(R.drawable.ic_action_btopen);
            // Update the bluetooth status text
            bluetoothStatusText.setText("Bluetooth is on.");
        }
        // If the bluetooth is disabled from the start
        else if (btAdapter.isEnabled() == false){
            // Change bluetooth image to be "btclosed"
            bluetoothImage.setImageResource(R.drawable.ic_action_btclosed);
            // Update the bluetooth status text
            bluetoothStatusText.setText("Bluetooth is disabled.");
        }

        // Register "ACTION_STATE_CHANGED" to be sent to the main broadcast
        // This will automatically inform the activity if the bluetooth turns on or off
        MainBroadcastReceiver mainBroadcastReceiver = new MainBroadcastReceiver();
        registerReceiver(mainBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Get paired devices on startup
        pairedDevices = btAdapter.getBondedDevices();
        // Show paired devices on startup for each device
        for(BluetoothDevice device : pairedDevices){
            btArrayAdapter.add(" Name: " + device.getName() + "\nAdress: " + device.getAddress());
        }

        //region -Initialize the handler
        // Create a main activity handler for updating gui elements etc.
        mainHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                // Check if message is about connection status
                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1){
                        // Update the connection status text
                        connectionStatusText.setText("Could not secure a connection!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Could not secure a connection!", Toast.LENGTH_SHORT).show();
                    }
                    else if(msg.arg1 == 2){
                        // Update the connection status text
                        connectionStatusText.setText("Securing connection...");
                    }
                    else if(msg.arg1 == 3){
                        // Update the connection status text
                        connectionStatusText.setText("Connection failed!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Connection failed!", Toast.LENGTH_SHORT).show();
                    }
                    else if(msg.arg1 == 4){
                        // Update the connection status text
                        connectionStatusText.setText("Connected to device!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Connected to device!", Toast.LENGTH_SHORT).show();

                        // Update the connected device name global variable
                        BluetoothVariables.connectedDeviceName = (String)msg.obj;
                        // Try to get the input and output streams and assign them to global variables
                        try{
                            // Get input and output streams
                            BluetoothVariables.inStream = BluetoothVariables.btSocket.getInputStream();
                            BluetoothVariables.outStream = BluetoothVariables.btSocket.getOutputStream();

                            // Create an intent to launch "SendReceiveActivity"
                            Intent successfulConnection = new Intent(getApplicationContext(), SendReceiveActivity.class);
                            // Launch "SendReceiveActivity"
                            startActivity(successfulConnection);
                        }catch (IOException e){
                            // Update the connection status text
                            connectionStatusText.setText("Something went wrong!");
                            // Show a toast message
                            Toast.makeText(getApplicationContext(), "Something went wrong!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else if(msg.arg1 == 5){
                        // Update the connection status text
                        connectionStatusText.setText("Connection lost!");
                        // Show a toast message
                        Toast.makeText(getApplicationContext(), "Connection lost!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        //endregion

        //region -On click listener for the "turn on bluetooth" button
        bluetoothOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if bluetooth is already on
                if(btAdapter.isEnabled() == true){
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Bluetooth is already on!", Toast.LENGTH_SHORT).show();
                }
                // If the bluetooth is off
                else{
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Turning on bluetooth...", Toast.LENGTH_SHORT).show();
                    // Create the intent with the bluetooth enable request
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    // Star activity for result with the bluetooth enable request intent
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        });
        //endregion

        //region -On click listener for the "turn off bluetooth" button
        bluetoothOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if bluetooth is on
                if(btAdapter.isEnabled() == true) {
                    // Disable bluetooth
                    btAdapter.disable();
                }
                // If the bluetooth is already off
                else{
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Bluetooth is already off!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //endregion

        //region -On click listener for the "reset" button for paired devices
        resetAvailableDevicesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the bluetooth is not on
                if(btAdapter.isEnabled() == false){
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
        //endregion

        //region -On click listener for the click of paired devices list
        availableDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // If the thread in this method is already running, exit
                if(threadSpamProtection == true){return;}
                // Check if the bluetooth is not on
                if (btAdapter.isEnabled() == false) {
                    // Update the connection status text
                    connectionStatusText.setText("Connection is not possible!");
                    // Show a toast message
                    Toast.makeText(getApplicationContext(), "Bluetooth is not on!", Toast.LENGTH_SHORT).show();
                    // Don't do anything and return
                    return;
                }
                // Update the connection status text
                connectionStatusText.setText("Connecting to device...");

                // Get the pressed items text as string
                String deviceInfo = ((TextView) view).getText().toString();
                // Separate the device's name and address(last 17 characters are the device address!)
                final String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);
                final String deviceName = deviceInfo.substring(7, deviceInfo.length() - 26);

                // Turn the thread protection on
                threadSpamProtection = true;

                // Start a new thread to not block gui
                new Thread() {
                    public void run() {
                        // Get a BluetoothDevice object for the given Bluetooth hardware address
                        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
                        // Try to create a connection
                        try {
                            // Create secure outgoing connection with BT device using UUID
                            BluetoothVariables.btSocket = device.createRfcommSocketToServiceRecord(btUUID);
                        } catch (IOException e) {
                            // Send the connection status to the handler
                            mainHandler.obtainMessage(CONNECTING_STATUS, 1, -1, null).sendToTarget();
                            // Enable the thread spam protection
                            threadSpamProtection = false;
                            // Don't do anything and return
                            return;
                        }
                        // Try to establish a connection
                        try {
                            // Send the connection status to the handler
                            mainHandler.obtainMessage(CONNECTING_STATUS, 2, -1, null).sendToTarget();
                            // Establish the connection
                            BluetoothVariables.btSocket.connect();
                        } catch (IOException e) {
                            // If connection fails try to close the connection
                            try {
                                // Send the connection status to the handler
                                mainHandler.obtainMessage(CONNECTING_STATUS, 3, -1, null).sendToTarget();
                                // Close the connection
                                BluetoothVariables.btSocket.close();
                                // Enable the thread spam protection
                                threadSpamProtection = false;
                                // Don't do anything and return
                                return;
                            } catch (IOException e2) {
                                // Send the connection status to the handler
                                mainHandler.obtainMessage(CONNECTING_STATUS, 3, -1, null).sendToTarget();
                                // Enable the thread spam protection
                                threadSpamProtection = false;
                                // Don't do anything and return
                                return;
                            }
                        }
                        // Send the connection status to the handler
                        mainHandler.obtainMessage(CONNECTING_STATUS, 4, -1, deviceName).sendToTarget();
                        // Disable the thread spam protection
                        threadSpamProtection = false;
                    }
                }.start();
            }
        });
        //endregion

    }// End of the onCreate!

    // Result check for "startActivityForResult" function
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // Check if the request was "bluetooth enable request"
        if(requestCode == REQUEST_ENABLE_BT){
            // Check if the enable was successful
            if(resultCode == RESULT_OK){
                // Do nothing
            }
            else if(resultCode == RESULT_CANCELED){
                // Update the bluetooth status text
                bluetoothStatusText.setText("Failed to turn on bluetooth.");
                // Show a toast message
                Toast.makeText(getApplicationContext(), "Failed to turn on bluetooth!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //region -Create a main broadcast receiver
    public class  MainBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the action from intent that was used to call the broadcast
            String action = intent.getAction();
            // If the action is bluetooth's state changed
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                if(btAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF){
                    // Change bluetooth image to be "btclosed"
                    bluetoothImage.setImageResource(R.drawable.ic_action_btclosed);
                    // Update the bluetooth status text
                    bluetoothStatusText.setText("Bluetooth is off.");
                    // Update the connection status text
                    connectionStatusText.setText("Connection is not possible!");
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
                    Toast.makeText(getApplicationContext(), "Bluetooth is turned on!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    //endregion

}// End of the main activity class!
