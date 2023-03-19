package com.example.CapstoneProjectCommunicationApp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.IOException;

@SuppressLint("all")
@SuppressWarnings("all")

public class SendReceiveActivity extends AppCompatActivity {

    //region -Create class variables
    // Gui variables
    private TextView logText;
    private TextView connectedDeviceText;
    private Button clearLogsBtn;
    private Button sendBtn;
    private Button startBtn;
    private Button stopBtn;
    private Button plusBtn;
    private Button minusBtn;
    private EditText singleSendTextInput;
    private EditText continuousSendTextInput;
    private EditText delayTextInput;

    // Create activity handler variable
    static public Handler sendReceiveHandler;

    // Create named constants to carry a message
    private final int Message_Received_Call = 1;
    private final int Message_Sent_Call = 2;
    private final int Continuous_Send_Started = 4;
    private final int Continuous_Send_Stopped = 5;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_recieve);

        //region -Assign GUI variables
        logText = (TextView) findViewById(R.id.logText);
        connectedDeviceText = (TextView) findViewById(R.id.connectedDeviceText);
        clearLogsBtn = (Button)findViewById(R.id.clearLogsBtn);
        sendBtn = (Button)findViewById(R.id.sendBtn);
        startBtn = (Button)findViewById(R.id.startBtn);
        stopBtn = (Button)findViewById(R.id.stopBtn);
        plusBtn = (Button) findViewById(R.id.plusBtn);
        minusBtn = (Button) findViewById(R.id.minusBtn);
        singleSendTextInput = (EditText)findViewById(R.id.singleSendTextInput);
        continuousSendTextInput = (EditText)findViewById(R.id.continuousSendTextInput);
        delayTextInput = (EditText)findViewById(R.id.delayTextInput);
        //endregion

        //region -Change the logtext configurations
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
        //endregion

        // Show the user connected device name
        connectedDeviceText.setText("(Connected to: " + BluetoothVariables.connectedDeviceName + ")");

        //region -Only do these if it's the first time activity is launching
        if (savedInstanceState == null) {
            // Tell user which device they are connected to in the beginning
            logText.append("\n***Connected to: " + BluetoothVariables.connectedDeviceName + "***");

            // Set the stop variables to false initially
            BluetoothVariables.stopReceiveThread = false;
            BluetoothVariables.stopContinuousThread = false;

            // Start receiving thread
            ReceiveThread receiveThread = new ReceiveThread();
            receiveThread.start();
        }
        //endregion

        //region -Only do these if the activity is restarting
        if (savedInstanceState != null) {
            // Recover the start and stop button enable states
            startBtn.setEnabled(savedInstanceState.getBoolean("Start Button"));
            stopBtn.setEnabled(!savedInstanceState.getBoolean("Start Button"));

            // Recover the log text
            String savedData = savedInstanceState.getString("Saved Log");
            logText.setText(savedData);
        }
        //endregion

        //region -Initialize the handler
        // Create a send receive activity handler for updating gui elements etc.
        sendReceiveHandler = new Handler(){
            public void handleMessage(android.os.Message msg) {
                if(msg.what == Message_Received_Call){
                    // Inform the user that a data was received
                    logText.append("\nData received: " + BluetoothVariables.bytes);
                }
                else if(msg.what == Message_Sent_Call){
                    if(msg.arg1 == 1) {
                        int sentData = (Integer) msg.obj;
                        // Inform the user that a data was sent
                        logText.append("\nData sent: " + sentData);
                    }
                    else if(msg.arg1 == 2){
                        // Inform the user that a data was sent
                        logText.append("\n***Couldn't send data!***");
                    }
                }
                else if(msg.what == Continuous_Send_Started){
                    // Inform the user that continuous send started
                    logText.append("\n***Continuous send started!***");
                }
                else if(msg.what == Continuous_Send_Stopped){
                    // Inform the user that continuous send stopped
                    logText.append("\n***Continuous send stopped!***");
                }
            }
        };
        //endregion

        //region -On click listener for the "clear logs" button
        clearLogsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the log text and inform the user
                logText.setText("***Cleared logs!***");
            }
        });
        //endregion

        //region -On click listener for the "send" button
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
                sendData(input);
            }
        });
        //endregion

        //region -On click listener for the "+" button
        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Create a variable to hold the input
                int input;

                // Put parsing into try to detect if user presses button with no input
                try {
                    // Parse the text to an integer variable
                    input = Integer.parseInt(singleSendTextInput.getText().toString());
                }catch (NumberFormatException e){
                    // If the text is empty set the text to "0"
                    singleSendTextInput.setText("0");
                    //Exit the event
                    return;
                }
                // Don't let the user go over 255
                if(input == 255){
                    // Inform the user
                    logText.append("\n***Input must be between 0-255!***");
                    // Exit the event
                    return;
                }
                // Increment the text
                singleSendTextInput.setText(""+(input+1));
            }
        });
        //endregion

        //region -On click listener for the "-" button
        minusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Create a variable to hold the input
                int input;

                // Put parsing into try to detect if user presses button with no input
                try {
                    // Parse the text to an integer variable
                    input = Integer.parseInt(singleSendTextInput.getText().toString());
                }catch (NumberFormatException e){
                    // If the text is empty set the text to "0"
                    singleSendTextInput.setText("0");
                    //Exit the event
                    return;
                }
                // Don't let the user go over 255
                if(input == 0){
                    // Inform the user
                    logText.append("\n***Input must be between 0-255!***");
                    // Exit the event
                    return;
                }
                // Increment the text
                singleSendTextInput.setText(""+(input-1));

            }
        });
        //endregion

        //region -On click listener for the "start" button
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
                BluetoothVariables.stopContinuousThread = false;
                // Start the continuous data send thread
                continuousSend(input, delay);

                // Make button enabled configurations
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                continuousSendTextInput.setEnabled(false);
                delayTextInput.setEnabled(false);
            }
        });
        //endregion

        //region -On click listener for the "stop" button
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the thread stop condition to true
                BluetoothVariables.stopContinuousThread = true;

                // Make button enabled configurations
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                continuousSendTextInput.setEnabled(true);
                delayTextInput.setEnabled(true);
            }
        });
        //endregion

    }// End of the onCreate

    // Make a class that extends a thread class to pool for receiving data
    class ReceiveThread extends Thread{
        public void run() {
            // Loop until stop condition is set
            while(BluetoothVariables.stopReceiveThread == false){
                // Try to read the data
                try{
                    // Read the data(this function blocks until it receives something to read)
                    BluetoothVariables.bytes = BluetoothVariables.inStream.read();
                    // Inform the user that data was received
                    SendReceiveActivity.sendReceiveHandler.obtainMessage(Message_Received_Call).sendToTarget();
                }catch (IOException e){
                    // Coming down here means the connection was lost

                    // Send a message to the main handler
                    MainActivity.mainHandler.obtainMessage(3,5,-1).sendToTarget();
                    // Finish the activity
                    finish();
                    // Break out of the infinite while loop
                    break;
                }
            }
        }
    }

    // Class method to send the data
    public boolean sendData(int input) {
        // Try to send the data
        try {
            // Send the data
            BluetoothVariables.outStream.write(input);
            // Send a message to the handler to inform the user
            sendReceiveHandler.obtainMessage(Message_Sent_Call,1, -1, input).sendToTarget();
            // Return data was successfully sent
            return true;
        } catch (IOException e) {
            // Send a message to the handler to inform the user
            sendReceiveHandler.obtainMessage(Message_Sent_Call,2, -1, input).sendToTarget();
            // Return data couldn't be sent
            return false;
        }
    }

    // Create a method that uses another thread to send data continuously
    public void continuousSend(int input, int delay){
        // When the method is called inform the user
        sendReceiveHandler.obtainMessage(Continuous_Send_Started).sendToTarget();
        // Put the passed variables into final variables since methods require
        final int finalInput = input;
        final int finalDelay = delay;
        // Create a new thread
        new Thread(){
            public void run() {
                // Create a boolean to check if data was successfully sent
                boolean result;
                // Loop until stop condition is set
                while(true) {
                    // Send the data
                    result = sendData(finalInput);
                    // Check the stop condition
                    if(BluetoothVariables.stopContinuousThread == true || result == false){
                        sendReceiveHandler.obtainMessage(Continuous_Send_Stopped).sendToTarget();
                        return;
                    }
                    // Wait for the delay
                    SystemClock.sleep(finalDelay);
                }
            }
        }.start();
    }

    // When the activity is starting to get deleted from the stack(about to be terminated by whatever means)
    @Override
    public void onPause() {
        // Check if the activity is closing completely and not restarting
        if(isFinishing() == true) {
            // Try to stop the threads and close the in/out streams and socket
            try {
                // Set the thread stop conditions for every thread
                BluetoothVariables.stopReceiveThread = true;
                BluetoothVariables.stopContinuousThread = true;

                // Close in/out streams then the socket(order is important)
                BluetoothVariables.inStream.close();
                BluetoothVariables.outStream.close();
                BluetoothVariables.btSocket.close();
            } catch (IOException e) {
                // This part only exists because the methods call for it
            }
        }

        super.onPause();
    }

    // If the activity is restarting and not closing completely save some data
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save the log text
        outState.putString("Saved Log", logText.getText().toString());
        // Save the start button condition(don't need stop button condition since it's always the opposite)
        outState.putBoolean("Start Button", startBtn.isEnabled());

        super.onSaveInstanceState(outState);
    }

}// End of the send receive activity class!
