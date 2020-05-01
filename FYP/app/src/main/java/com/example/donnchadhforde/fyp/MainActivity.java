package com.example.donnchadhforde.fyp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BTapp";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int BT_DEVICE_SELECTED = 2;
    int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mDevice;
    BluetoothService btService;
    private String readMessage;

    private DrawerLayout drawerLayout;
    private FrameLayout assessmentFragment;
    FragmentTest fragTest;

    private OpenGLView openGLView;

    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.8F);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assessmentFragment = findViewById(R.id.content_frame);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        openGLView = (OpenGLView) findViewById(R.id.openGLView);

        drawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        drawerLayout.closeDrawers();

                        switch(menuItem.toString()) {
                            case("Profile"):
                                //Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                                //startActivity(profileIntent);
                                break;

                            case("Calibrate"):
                                btService.write(Constants.CALIBRATE);
                                Log.d(TAG, "here");
                                break;

                            case("Assessment"):
                                Log.d(TAG, "Assessment clicked");
                                btService.write(Constants.SESSION_ACTIVE);
                                Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                                startActivity(profileIntent);
//                                if(!(btService == null)) {
//                                    if (btService.getState() == BluetoothService.STATE_CONNECTED) {
//                                        Log.d(TAG, "HERE");
//                                FragmentTest ftest = FragmentTest.newInstance(readMessage);
//                                FragmentManager fm = getSupportFragmentManager();
//                                FragmentTransaction ft = fm.beginTransaction();
//                                ft.addToBackStack(null);
//                                ft.add(R.id.content_frame, ftest).commit();
                                //openAssessmentFragment();
//                                    }
//                                } else {
//                                    Toast.makeText(MainActivity.this, "No Device Connected", Toast.LENGTH_SHORT).show();
//                                }
                                break;

                            case("Logout"):
                                break;

                            default:
                                break;
                        }

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
                    }
                });

        checkPermissions();
        enableBluetooth();

    }

    public BluetoothService getBtService() {
        return this.btService;
    }

    public void setDataLabel(String text) {
        final TextView myLabel = (TextView)findViewById(R.id.dataLabel);
        myLabel.setText(text);
    }
//
//    public void setMyLabel(String text) {
//        final TextView myLabel = (TextView)findViewById(R.id.myLabel);
//        myLabel.setText(text);
//    }

    public void openAssessmentFragment() {
        AssessmentFragment fragment = AssessmentFragment.newInstance(mDevice.getName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.addToBackStack(null);
        transaction.add(R.id.content_frame, fragment, "ASSESSMENT").commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    public void scanButton(View view) {
        view.startAnimation(buttonClick);

        Intent scanIntent = new Intent(this, DeviceScanActivity.class);
        startActivityForResult(scanIntent, BT_DEVICE_SELECTED);
    }

    //public void assessmentButton(View view) {

        //if (btService == null) {
          //  Toast.makeText(this, "No Device Connected", Toast.LENGTH_SHORT).show();
        //} else if (btService.getState() == BluetoothService.STATE_CONNECTED) {

//            Intent assessmentIntent = new Intent(this, AssessmentActivity.class);
//            assessmentIntent.putExtra("btDevice", mDevice);
//            startActivity(assessmentIntent);
        //}
    //}

    public void connectDevice(BluetoothDevice device) {

        btService = new BluetoothService(device, mHandler);
        Log.d(TAG, device.getName());

        btService.connect(device);

        while(btService.getState() == BluetoothService.STATE_CONNECTING) {};

        if (btService.getState() == BluetoothService.STATE_CONNECTED) {
            String deviceName = btService.getDevice().getName();
            Log.d(TAG, "Session button");
            Toast.makeText(this, "Connected to: " + deviceName, Toast.LENGTH_SHORT).show();
            //connectedDevice = mdevice;
            //Log.d(TAG, myLabel.getText()):

            //setMyLabel("Connected: " + deviceName);
        }
    }

    public void enableBluetooth() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Log.d(TAG, "Device does not support Bluetooth");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Bluetooth On");
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Bluetooth not enabled by user");
                onDestroy();
            }
        } else if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                mDevice = data.getExtras().getParcelable("btDevice");
                connectDevice(mDevice);
                Log.d(TAG, "Bluetooth Device Selected");
                //connectDevice();
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "No Device Selected");
            }
        }
    }

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch(msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch(msg.arg1) {

                    }

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    readMessage = new String(readBuf, 0, msg.arg1);
                    Bundle bundle = new Bundle();
                    bundle.putString("btData", readMessage);
                    FragmentTest ft = new FragmentTest();
                    ft.setArguments(bundle);
                    setDataLabel(readMessage);

            }
        }

    };


    @Override
    protected void onResume() {
        super.onResume();
        //enableBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        Log.d(TAG, "Closed");
    }

}
