package com.example.donnchadhforde.fyp;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

//import static com.example.donnchadhforde.fyp.MainActivity.myLabel;

public class DeviceScanActivity extends AppCompatActivity {

    private static final String TAG = "DeviceScanActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private ListView pairedListView, newDeviceListView;
    private ArrayList<String> uniqueDevices = new ArrayList<String>();
    private ArrayAdapter<String> pairedDevicesAdapter;
    private ArrayAdapter<String> newDevicesAdapter;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private Menu menu;
    public BluetoothDevice connectedDevice;
    public BluetoothService btService;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

//        pairedListView = (ListView) findViewById(R.id.paired_device_list);
//        pairedDevicesAdapter = new ArrayAdapter<String>(
//                this, R.layout.device_details);
//        pairedListView.setAdapter(pairedDevicesAdapter);
//        registerForContextMenu(pairedListView);

        newDeviceListView = (ListView) findViewById(R.id.new_device_list);
        newDevicesAdapter = new ArrayAdapter<String>(
                this, R.layout.device_details, uniqueDevices);
        newDeviceListView.setAdapter(newDevicesAdapter);
        registerForContextMenu(newDeviceListView);

//        uniqueDevices.add("Test");
        newDevicesAdapter.notifyDataSetChanged();
//        uniqueDevices.remove("Test");
//        listAdapter.notifyDataSetChanged();
        //mHandler = new Handler();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

//        if (!pairedDevices.isEmpty()) {
//            for (BluetoothDevice device : pairedDevices) {
//                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
//            }
//        } else {
//            pairedDevicesAdapter.add("No Paired Devices");
//        }
    }

    public void DiscoverDevices() {
        //mScanning = true;
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter.startDiscovery();
            Toast.makeText(this, "Discovering Devices...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Bluetooth disabled or unavailable", Toast.LENGTH_SHORT).show();
        }

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "---Discovery Started---");
                mScanning = true;
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Discovery has found a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevices.add(device);
                if(!(device.getName() == null)) {
                    uniqueDevices.add(device.getName() + "\n" + device.getAddress());
                } else {
                    uniqueDevices.add("Unnamed Device" + "\n" + device.getAddress());
                }
                newDevicesAdapter.notifyDataSetChanged();
            }

            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "---Discovery Finished---");
                mScanning = false;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.action_bar_indeterminate_progress);
        }
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "called");
        updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mScanning = true;
                //mLeDeviceListAdapter.clear();
                DiscoverDevices();
                updateMenu();
                Log.d(TAG, "menu scan selected");
                break;

            case R.id.menu_stop:
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                mScanning = false;
                updateMenu();
                break;

            case R.id.menu_refresh:
                uniqueDevices.clear();
                mDevices.clear();
                pairedDevicesAdapter.notifyDataSetChanged();
                newDevicesAdapter.notifyDataSetChanged();
                mScanning = true;
                DiscoverDevices();
                updateMenu();
                break;
        }
        return true;
    }

    private void updateMenu() {
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.action_bar_indeterminate_progress);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.connect:
                int pos = info.position;
                BluetoothDevice btDevice = mDevices.get(pos);
                Log.d(TAG, btDevice.getName());

                Intent intent = new Intent();
                intent.putExtra("btDevice", btDevice);
                setResult(RESULT_OK, intent);
                finish();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();

        }

    }

//    public void connectToDevice(BluetoothDevice device) {
//
//        btService = new BluetoothService(device, mHandler);
//        btService.connect(device);
//        if(btService.getState() == BluetoothService.STATE_CONNECTED) {
//            String deviceName = btService.getDevice().getName();
//            Toast.makeText(this, "Connected to: " + deviceName, Toast.LENGTH_SHORT).show();
//            connectedDevice = device;
//            //myLabel.setText("Connected: " + deviceName);
//        }
//        finish();
//
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

}

