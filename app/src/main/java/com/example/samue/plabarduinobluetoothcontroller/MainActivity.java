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
import static android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED;


public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 123;
    private BluetoothAdapter myBluetooth;
    TextView connectionTextView;
    CardView statusCardView;
    Button btnPairedList, btnOpenBtSettings;
    public static String EXTRA_ADDRESS = "com.samue.plabarduinobluetoothcontroller"; // for intent
    public static String EXTRA_DEVICE_NAME = "com.samue.devicename";
    private String deviceName;
    private IntentFilter filterBluetoothDevice, filterBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v("Main OnCreate", "--");


        //OnClickListener setup with method calls ----------------------------------------------
        statusCardView = (CardView) findViewById(R.id.card_status_connection);
        statusCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btTurnOn();
            }
        });

        btnPairedList = (Button) findViewById(R.id.btn_show_devices);
        btnPairedList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPairedDevicesList();
            }
        });

        btnOpenBtSettings = (Button) findViewById(R.id.btn_open_bt_settings);
        btnOpenBtSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBtSettings();
            }
        });
        // --------------------------------------------------------------------------------------

        // IntentFilter to register changes in bluetooth status



        filterBluetoothDevice = new IntentFilter();
        filterBluetoothDevice.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filterBluetoothDevice.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        filterBluetoothAdapter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

        this.registerReceiver(mBluetoothDeviceReceiver, filterBluetoothDevice);
        this.registerReceiver(mBluetoothAdapterReceiver, filterBluetoothAdapter);


        // Initial setup of status information --------------------------------------------------
        connectionTextView = (TextView) findViewById(R.id.txt_connection_status);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        updateStatusTextView();
    }

    private void updateStatusTextView() {
        if (myBluetooth == null) {
            Toast.makeText(getApplicationContext(), "No bluetooth adapter found", Toast.LENGTH_LONG).show();
            connectionTextView.setText(getString(R.string.status_card_view_not_available));
        } else if (!myBluetooth.isEnabled()) {
            connectionTextView.setText(getString(R.string.status_card_view_off));
        } else {
            connectionTextView.setText(getString(R.string.status_card_view_on));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v("Main", "onpause");
        this.unregisterReceiver(mBluetoothDeviceReceiver);
        this.unregisterReceiver(mBluetoothAdapterReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("Main", "onresume");
        this.registerReceiver(mBluetoothDeviceReceiver, filterBluetoothDevice);
        this.registerReceiver(mBluetoothAdapterReceiver, filterBluetoothAdapter);
        updateStatusTextView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("Main", "onstop");
        try {
            this.unregisterReceiver(mBluetoothDeviceReceiver);
            this.unregisterReceiver(mBluetoothAdapterReceiver);
        } catch (RuntimeException e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("Main", "onstop");
        try {
            this.unregisterReceiver(mBluetoothDeviceReceiver);
            this.unregisterReceiver(mBluetoothAdapterReceiver);
        } catch (RuntimeException e) {
        }
    }

    private final BroadcastReceiver mBluetoothAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); // gets the action (ACTION_ACL_CONNECTED etc..)
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                Log.v("MAIN BTAdapterRec", "action is STATE CHANGED " + action + "\nMatches: " + BluetoothAdapter.ACTION_STATE_CHANGED);
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        connectionTextView.setText(getString(R.string.status_card_view_off));
                        break;
                    //Not necessary?
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        connectionTextView.setText(getString(R.string.status_card_view_off));
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        connectionTextView.setText(getString(R.string.status_card_view_on));
                        break;
                    case BluetoothAdapter.STATE_ON:
                        connectionTextView.setText(getString(R.string.status_card_view_on));
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
            Log.v("mBluetoothDeviceRec", "action is::: " + action);
            switch (action) {
                // BT is on and connected
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.v("MAINBluetoothDeviceRec", "action is ACL CONNECTED  " + action);
                    deviceName = device.getName();
                    String statusMessage = getString(R.string.status_card_view_connected, deviceName);
                    connectionTextView.setText(statusMessage);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.v("MAINBluetoothDeviceRec", "action is ACL DISCONNECTED " + action);
                    connectionTextView.setText(getString(R.string.status_card_view_on));
                    break;
            }
        }
    };

    private void btTurnOn() {
        if (!myBluetooth.isEnabled()) { //Bluetooth not enabled, ask user to turn on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                connectionTextView.setText(getString(R.string.status_card_view_on));
            } else {
                connectionTextView.setText(getString(R.string.status_card_view_error));
            }
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