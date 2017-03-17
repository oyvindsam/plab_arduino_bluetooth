package com.example.samue.plabarduinobluetoothcontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import java.io.IOException;
import java.util.UUID;

import static android.R.attr.action;
import static android.R.attr.filter;
import static com.example.samue.plabarduinobluetoothcontroller.R.id.btnOff;
import static com.example.samue.plabarduinobluetoothcontroller.R.id.btnOn;
import static com.example.samue.plabarduinobluetoothcontroller.R.id.btn_led_toggle;
import static com.example.samue.plabarduinobluetoothcontroller.R.layout.dialog;

/*Current functionality:
- user has chosen bt device to connect to, tries to connect
- user interacts with bt device
- if phone cannot communicate with bt device (when doing an action) --> tries to reconnect, if not exits activity
- if bt is turned off by user? exit or stay on activity and ask to connect?...
 */

public class LedControl extends AppCompatActivity {
    Button btnLedToggle, btnDisconnect, btnSendCommand;
    SeekBar brightness;
    CardView cardView;
    TextView progressTxt, cardStatus;
    String address, deviceName;
    Intent newInt;
    private int ledStatus = 0;

    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private IntentFilter filterBluetoothDevice, filterBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_control);
        Log.v("Led", "OnCreate");


        newInt = getIntent();
        address = newInt.getStringExtra(MainActivity.EXTRA_ADDRESS);
        deviceName = newInt.getStringExtra(MainActivity.EXTRA_DEVICE_NAME);
        Log.v("LeActivity", "This is the device name: " + deviceName + "-----------");

        cardStatus = (TextView) findViewById(R.id.txt_connection_status);
        cardStatus.setText(getString(R.string.status_card_view_cp, deviceName));

        cardView = (CardView) findViewById(R.id.card_control_panel_card_view);
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btTurnOn();
            }
        });

        btnSendCommand = (Button) findViewById(R.id.btn_send_command);
        btnSendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand();
            }
        });

        btnLedToggle = (Button) findViewById(btn_led_toggle);
        btnLedToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLed();
            }
        });

        btnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });


        // IntentFilter to register changes in bluetooth status
        filterBluetoothDevice = new IntentFilter();
        filterBluetoothDevice.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filterBluetoothDevice.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        filterBluetoothAdapter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

        this.registerReceiver(mBluetoothDeviceReceiver, filterBluetoothDevice);
        this.registerReceiver(mBluetoothAdapterReceiver, filterBluetoothAdapter);

        new ConnectBT().execute(); //Call the class to connect

    }

    private void sendMessage(String s) {
        s = s + "\r\n";
        byte[] buffer = new byte[s.length()];
        for (int i=0; i<s.length();i++){
            buffer[i] = (byte) s.charAt(i);
        }
        try {
            btSocket.getOutputStream().write(buffer, 0, s.length());
        } catch (IOException e) {
            msg(getString(R.string.error_bt_socket));
            new ConnectBT().execute(); //Call the class to try to reconnect
        }
    }

    private void sendMessageInt(String s, int i) {
        sendMessage(s + "," + i);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("Led", "onstop");
        try {
            this.unregisterReceiver(mBluetoothDeviceReceiver);
            this.unregisterReceiver(mBluetoothAdapterReceiver);
        } catch (RuntimeException e) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v("Led", "OnPause");
        this.unregisterReceiver(mBluetoothDeviceReceiver);
        this.unregisterReceiver(mBluetoothAdapterReceiver);
        if ( progress!=null && progress.isShowing() ) {
            Log.v("Led", "OnDPause -- progress.dismiss()");

            progress.dismiss();
            progress = null;
        }
        if ( progress!=null && progress.isShowing() ){
            Log.v("Led", "onPause -- progress.dismiss()");
            progress.dismiss();
            progress = null;
        }
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                msg(getString(R.string.error_bt_socket));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("Led", "OnResume");
        this.registerReceiver(mBluetoothDeviceReceiver, filterBluetoothDevice);
        this.registerReceiver(mBluetoothAdapterReceiver, filterBluetoothAdapter);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.v("Led", "OnDestroy");
        try {
            this.unregisterReceiver(mBluetoothDeviceReceiver);
            this.unregisterReceiver(mBluetoothAdapterReceiver);
        } catch (RuntimeException e) {
        }
        if ( progress!=null && progress.isShowing() ){
            Log.v("Led", "OnDestroy -- progress.dismiss()");

            progress.dismiss();
            progress = null;
        }
    }

    //TODO put these classes in their own file ---------------------------------------------------

    //The BroadcastReceiver that listens for bluetooth broadcasts
    // Goal for this code: check bluetooth status and give user feedback on:
    // 1. [on, not connected], 2. [on, connected to "device"], 3. [off]
    private final BroadcastReceiver mBluetoothAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); // gets the action (ACTION_ACL_CONNECTED etc..)
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                Log.v("LED--initial--BTadapte", "action is STATE CHANGED " + action + "\nMatches: " + BluetoothAdapter.ACTION_STATE_CHANGED);
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Log.v("BluetoothAdapterRec", "action is STATE CHANGED and: == " + state);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        cardStatus.setText(getString(R.string.status_card_view_off));
                        break;
                    //Not necessary?
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        cardStatus.setText(getString(R.string.status_card_view_off));
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        cardStatus.setText(getString(R.string.status_card_view_on));
                        break;
                    case BluetoothAdapter.STATE_ON:
                        cardStatus.setText(getString(R.string.status_card_view_on));
                        break;

                }
            }
        }
    };

    private final BroadcastReceiver mBluetoothDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); // gets the action (ACTION_ACL_CONNECTED etc..)
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.v("LED--initial receive--", "action is== " + action);
            switch (action) {
                // BT is on and connected
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.v("LED--ACL-Con-- ", "action is==  " + action);
                    deviceName = device.getName();
                    String statusMessage = getString(R.string.status_card_view_connected, deviceName);
                    cardStatus.setText(statusMessage);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.v("LED--ACL-DisCon--", "action== " + action);
                    cardStatus.setText(getString(R.string.status_card_view_on));
                    break;
            }
        }
    };

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean connectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(LedControl.this, getString(R.string.progress_dialog_connecting, deviceName), getString(R.string.progress_dialog_please_wait));
        }

        @Override
        protected Void doInBackground(Void... devices) {
            Log.v("Led", "start doInBackground---");
            isBtConnected = false;
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter(); // get the mobile bt device
                    BluetoothDevice connectedDevice = myBluetooth.getRemoteDevice(address); //connects to the devices address and checks if it's available
                    btSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    Log.v("Led", "Before connect()");

                    btSocket.connect(); // Error here when failing to connect. Happens on PC?
                    Log.v("Led", "After connect()");

                }
            } catch (IOException e) {
                Log.v("Led", "IOException after connect()");
                connectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.v("Led", "OnPostExecute ----");

            if (!connectSuccess) {
                msg("Connection failed");
                Log.v("Led", "Before finish()");
                finish();
            } else {
                Log.v("Led", "Connected --");

                msg("Connected");
                isBtConnected = true;
            }
            Log.v("Led", "Before dismiss()");
            if ( progress!=null && progress.isShowing() ){
                Log.v("Led", "OnDestroy -- progress.dismiss()");

                progress.dismiss();
                progress = null;
            }
        }
    }



    public void sendCommand() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText deviceNameEditText = (EditText) dialogView.findViewById(R.id.alert_dialog_name_text_view);
        dialogBuilder.setTitle(getString(R.string.alert_dialog_header_command));
        dialogBuilder.setMessage(getString(R.string.alert_dialog_message_command));
        dialogBuilder.setPositiveButton(getString(R.string.alert_dialog_done), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String command = deviceNameEditText.getText().toString().toUpperCase();
                sendMessage(command);
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.alert_dialog_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void disconnect() {
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                msg(getString(R.string.error_bt_socket));
            }
        }
        finish(); // Return to the first layout
    }


    public void toggleLed() {
        switch (ledStatus) {
            case 0:
                ledStatus = 1;
                break;
            case 1:
                ledStatus = 0;
                break;
        }
        sendMessageInt("LEDON", ledStatus);

    }

    private void btTurnOn() {
        if (!myBluetooth.isEnabled()) { //Bluetooth not enabled, ask user to turn on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }
    }
}





