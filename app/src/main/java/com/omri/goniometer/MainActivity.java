package com.omri.goniometer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.security.Permission;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    public final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int DEVICE_FOUND = 2;
    public final static int REQUEST_PERMISSIONS = 3; // used in bluetooth handler to identify message status

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int DATA_RECEIVED = 3;
    public static final int BATTERY_UPDATE = 4;


    Bluetooth mBluetooth;
    PermissionsClass mPermissions;
    private TextView mBluetoothStatus;
    private CheckBox mCheckBox;
    private Button mRefreshBtn;
    private TextView mAngle;
    private TextView mPitch;
    boolean connectedStatusUpdated;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplication().getApplicationContext();
        connectedStatusUpdated = false;
        mBluetoothStatus = (TextView) findViewById(R.id.bluetoothStatus);
        mCheckBox = (CheckBox) findViewById(R.id.checkBox);
        mCheckBox.setEnabled(false);
        mCheckBox.setText("Goniometer");
        mBluetoothStatus.setText("Bluetooth Status");
        mBluetoothStatus.setTextColor(Color.BLACK);
        mRefreshBtn = (Button) findViewById(R.id.refresh);
        mAngle = (TextView) findViewById(R.id.Angle);
        mPitch = (TextView) findViewById(R.id.Pitch);

        mCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCheckBox.isChecked()) {
                    mBluetooth.connect();
                }
            }
        });

        mRefreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                mBluetooth.disconnect();
                mBluetooth.close();
                mPermissions.askPermissions();
                mBluetooth.searchDevice();
                Toast.makeText(MainActivity.this, "Scanning devices", Toast.LENGTH_SHORT).show();
            }
        });

        mPermissions = new PermissionsClass(this,this);
        mPermissions.askPermissions();
        mBluetooth = new Bluetooth(this, this);
    }


    public void updatesStatus(final int status, final String roll,final String pitch){

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (status == DEVICE_FOUND) {
                    mCheckBox.setText("Goniometer detected");
                    mCheckBox.setEnabled(true);
                }

                else if (status == STATE_CONNECTED) {

                }

                else if (status == DATA_RECEIVED){
                    if (!connectedStatusUpdated){
                        connectedStatusUpdated = true;
                        mBluetoothStatus.setText("Bluetooth connected");
                        mBluetoothStatus.setTextColor(Color.BLUE);
                        mCheckBox.setEnabled(false);
                        mRefreshBtn.setClickable(false);
                    }
                    mAngle.setText(roll + "\u00B0");
                    mPitch.setText(pitch + "\u00B0");
                }

                else if (status == STATE_DISCONNECTED){
                    mBluetoothStatus.setText("Bluetooth disconnected");
                    mBluetoothStatus.setTextColor(Color.RED);
                    mCheckBox.setText("Goniometer");
                    mCheckBox.setChecked(false);
                    mCheckBox.setEnabled(false);
                    mRefreshBtn.setClickable(true);
                    connectedStatusUpdated = false;
                    mBluetooth.disconnect();
                    mBluetooth.close();
                }
                else if (status == STATE_CONNECTING){
                    mBluetoothStatus.setText("Attempting to connect...");
                    mBluetoothStatus.setTextColor(Color.BLACK);
                }
                else if (status == BATTERY_UPDATE){
                    //mBatStatus.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
                    //mBatStatus.setText(deciBattery + "%");
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Bluetooth enabled");
                mBluetoothStatus.setTextColor(Color.BLACK);
                mBluetooth.searchDevice();
            } else
                mBluetoothStatus.setText("Bluetooth disabled");
                mBluetoothStatus.setTextColor(Color.BLACK);
        }
        if (requestCode == DEVICE_FOUND){
            Toast.makeText(getApplicationContext(),"Goniometer detected", Toast.LENGTH_SHORT).show();
            mCheckBox.setText("Goniometer detected");
            mCheckBox.setEnabled(true);
        }
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE){
            mBluetooth.searchDevice();
        }
    }

    void createNewBle(){
        mBluetooth = new Bluetooth(this,this);
    }


}
