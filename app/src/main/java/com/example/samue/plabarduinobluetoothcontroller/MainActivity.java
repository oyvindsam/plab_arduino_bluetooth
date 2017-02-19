package com.example.samue.plabarduinobluetoothcontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.Set;
import java.util.ArrayList;

import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import static android.R.attr.action;
import static android.R.attr.filter;
import static android.R.id.message;
import static android.provider.Settings.Global.DEVICE_NAME;


public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter myBluetooth;
    TextView connectionTextView;
    public static String EXTRA_ADDRESS ="com.samue.plabarduinobluetoothcontroller"; // for intent
    public static String EXTRA_DEVICE_NAME = "com.samue.devicename";
    private String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //OnClickListener setup with method calls ----------------------------------------------
        CardView statusCardView = (CardView) findViewById(R.id.card_status_connection);
        statusCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btTurnOn();
            }
        });

        Button btnPairedList = (Button) findViewById(R.id.btn_show_devices);
        btnPairedList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPairedDevicesList();
            }
        });

        Button btnOpenBtSettings = (Button) findViewById(R.id.btn_open_bt_settings);
        btnOpenBtSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBtSettings();
            }
        });
        // --------------------------------------------------------------------------------------

        // IntentFilter to register changes in bluetooth status
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(new mReceiver(), filter);


        // Initial setup of status information --------------------------------------------------
        connectionTextView = (TextView) findViewById(R.id.txt_connection_status);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        if(myBluetooth == null) {
            Toast.makeText(getApplicationContext(), "No bluetooth adapter found", Toast.LENGTH_LONG).show();
            connectionTextView.setText(getString(R.string.status_card_view_not_available));
        } else if (!myBluetooth.isEnabled()){
            connectionTextView.setText(getString(R.string.status_card_view_off));
        } else {
            connectionTextView.setText(getString(R.string.status_card_view_on));
        }
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    // Goal for this code: check bluetooth status and give user feedback on:
    // 1. [on, not connected], 2. [on, connected to "device"], 3. [off]
    class mReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); // gets the action (ACTION_ACL_CONNECTED etc..)
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            // Bluetooth is turned off
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF
                        || intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_TURNING_OFF) {
                    connectionTextView.setText(getString(R.string.status_card_view_off));
                }

                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON
                    || intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_TURNING_ON) {
                    connectionTextView.setText(getString(R.string.status_card_view_on));
                }
            }
            // BT is on and connected
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                deviceName = device.getName();
                String statusMessage = getString(R.string.status_card_view_connected, deviceName);
                connectionTextView.setText(statusMessage);
            }
            // Bt is on but disconnected
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                connectionTextView.setText(getString(R.string.status_card_view_on));
            }
        }
    };

    private void btTurnOn() {
        if (!myBluetooth.isEnabled()) { //Bluetooth not enabled, ask user to turn on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }
    }

    void openBtSettings() {
        Intent turnBTon = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivityForResult(turnBTon, 1);
    }

    //Shows paired devices, if successful make a new list and start OnItemClickListener on
    // items in AdapterView (which is shown in a ListView).
    private void showPairedDevicesList() {
        Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
        ArrayList<String> list = new ArrayList<>();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                //Adds device name and address to list
                list.add(bt.getName() + "\n" + bt.getAddress());
            }
        } else {
            Toast.makeText(getApplicationContext(), "No paired bluetooth devices found", Toast.LENGTH_LONG).show();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, list);
        ListView deviceListView = (ListView) findViewById(R.id.list_paired_devices);
        deviceListView.setAdapter(adapter);
        deviceListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openControlPanel(view); // needs arg View to get text from the adapter (device MAC)
            }
        });
    }

    private void openControlPanel(View v) {
        //Get device MAC address, the last 17 chars in the View
        String info = ((TextView) v).getText().toString();
        String address = info.substring(info.length() - 17);
        String deviceName = info.substring(0, info.length() - 17);
        Log.v("MainActivity", "This is the MainActivity device name: " + deviceName + "------------");
        //Make intent to start a new activity
        Intent i = new Intent(MainActivity.this, LedControl.class);
        //Change the activity
        i.putExtra(EXTRA_ADDRESS, address); //This will be received at ledControl (class) activity
        i.putExtra(EXTRA_DEVICE_NAME, deviceName);
        startActivity(i);
    }

}
