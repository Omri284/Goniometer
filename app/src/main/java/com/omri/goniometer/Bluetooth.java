package com.omri.goniometer;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;

import java.util.List;
import java.util.UUID;


public class Bluetooth {
    private final Context mContext;
    private final MainActivity mActivity;
    //final String devcAddress = "D8:A0:1D:47:49:82"; //MyDeice
    final String devcAddress = "D8:A0:1D:51:B4:9A"; // ALYN device
//    private static final UUID ServiceUUID = UUID.fromString("0000183B-0000-1000-8000-00805F9B34FB");
//    private static final UUID CharUUID = UUID.fromString("00002A08-0000-1000-8000-00805F9B34FB");
    private static final UUID ServiceUUID = UUID.fromString("da3a95de-467b-11ea-b77f-2e728ce88125");
    private static final UUID CharUUID = UUID.fromString("da3a9836-467b-11ea-b77f-2e728ce88125");
    private String scanDevcAddress = null;
    private BluetoothAdapter mBTAdapter;
    private boolean mScanning;
    private static final long SCAN_PERIOD = 5000;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothDevice BTdevice;
    private BluetoothGatt mBTGatt;
    private BluetoothGattCharacteristic mGattChar;
    private int mConnectionState = STATE_DISCONNECTED;
    private Short rollInt = 0;
    private Short pitchInt = 0;
    String rollString = "0";
    String pitchString = "0";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int WAVE = 3;
    private static final int BATTERY_UPDATE = 4;
    String[] stringArray;
    Handler mHandler;
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    Bluetooth(Context context, MainActivity activity) {
        this.mContext = context;
        this.mActivity = activity;
        mHandler = new Handler();
        disconnect();
        close();

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBTAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBTAdapter.getBluetoothLeScanner();
        if (mBTAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(mContext, "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
        } else {

            if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(mContext, "no BLE feature in phone", Toast.LENGTH_SHORT).show();
            }

            searchDevice();

            //connect();

        }

    }

    public void searchDevice() {
        if (mBTAdapter.isEnabled()) {
            scanLeDevice(true);
        } else {
            Toast.makeText(mContext, "To begin please turn on bluetooth", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBtIntent, MainActivity.REQUEST_ENABLE_BT);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);
            mBTAdapter.disable();
            mBTAdapter.enable();
            mScanning = true;
            Log.d("ADebugTag", "Just before scan");
            mBluetoothLeScanner.startScan(leScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d("ADebugTag", "Inside scan callback scan");
            BTdevice = result.getDevice();
            scanDevcAddress = result.getDevice().getAddress();
            Log.d("ADebugTag", "scanned address: " + scanDevcAddress);
            if (scanDevcAddress.equals(devcAddress)) {
                mActivity.updatesStatus(MainActivity.DEVICE_FOUND,"","");
                //Intent deviceFoundIntent = new Intent(mActivity,MainActivity.class);
                scanLeDevice(false);

                //Toast.makeText(mContext, "Scanning stopped", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public boolean connect() {
        mActivity.updatesStatus(MainActivity.STATE_CONNECTING,"","");
        BTdevice = mBTAdapter.getRemoteDevice(devcAddress);
        mBTGatt = BTdevice.connectGatt(mContext, false, mGattCallback);
        if (mBTGatt == null){
            Log.d("ADebugTag", "mBTGatt is null");
            return false;
        }
        else {
            try {
                mGattChar = mBTGatt.getService(ServiceUUID).getCharacteristic(CharUUID);
            }
            catch (NullPointerException e){
                Log.d("ADebugTag", "mGattChar is null\n" + e);
            }
            //setCharacteristicNotification(mGattChar, true);
            return true;
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                mActivity.updatesStatus(MainActivity.STATE_CONNECTED,"","");
                Log.d("ADebugTag", "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.d("ADebugTag", "Attempting to start service discovery:" + mBTGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                mActivity.updatesStatus(MainActivity.STATE_DISCONNECTED,"","");
                Log.d("ADebugTag", "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                if (mBTGatt ==null){
                    return;
                }
                else {
                    List<BluetoothGattService> services = getSupportedGattServices();
                    for (BluetoothGattService service : services) {
                        if (!service.getUuid().equals(ServiceUUID))
                            continue;

                        List<BluetoothGattCharacteristic> gattCharacteristics =
                                service.getCharacteristics();

                        // Loops through available Characteristics.
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            if (!gattCharacteristic.getUuid().equals(CharUUID))
                                continue;

                            final int charaProp = gattCharacteristic.getProperties();

                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                setCharacteristicNotification(gattCharacteristic, true);
                            } else {
                                Log.d("ADebugTag", "Characteristic does not support notify");
                            }
                        }
                    }
                }
            } else {
                Log.d("ADebugTag", "onServicesDiscovered received:" + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                final byte[] dataInput = characteristic.getValue();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            final byte[] dataInput = characteristic.getValue();
            //Log.d("ADebugTag", "Data on changed: " + dataInput);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // For all other profiles, writes the data formatted in HEX.
        if (characteristic == null) {
            Log.d("ADebugTag", "characteristic is null in broadcastUpdate");
        }
        else {
            final byte[] data = characteristic.getValue();
            //Log.d("ADebugTag", "Data broadcast update: " + data);
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%X ", byteChar));
                //stringBuilder.append(String.format("%d ", byteChar));
                String ds = stringBuilder.toString();
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + ds);
                Log.d("ADebugTag", "Data complete string: " + ds);
                    stringArray = ds.split(" ");
                    rollInt = (short) Integer.parseInt(stringArray[3]+stringArray[2],16);
                    pitchInt = (short) Integer.parseInt(stringArray[1]+stringArray[0],16);
                    Log.d("ADebugTag", "Roll received:" + rollInt);
                    Log.d("ADebugTag", "Pitch received:" + pitchInt);
                    rollString = rollInt.toString();
                    pitchString = pitchInt.toString();
                    mActivity.updatesStatus(MainActivity.DATA_RECEIVED,rollString,pitchString);
                    //deciBattery = Integer.parseInt(bateryStringArray[0],16);
                    //updatesStatus(BATTERY_UPDATE);
            }
            mContext.sendBroadcast(intent);
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBTAdapter == null || mBTGatt == null) {
            Log.d("ADebugTag", "BluetoothAdapter not initialized in set char");
            return;
        }
        else {
            //mGattChar = mBTGatt.getService()
            mBTGatt.setCharacteristicNotification(characteristic, enabled);
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CharUUID);
//            descriptor.setValue(descriptor.ENABLE_NOTIFICATION_VALUE);
//            mBTGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBTAdapter == null || mBTGatt == null) {
            Log.d("ADebugTag", "BluetoothAdapter not initialized");
            return;
        }
        mBTGatt.disconnect();
    }
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBTGatt == null) return null;

        return mBTGatt.getServices();
    }
    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBTGatt == null) {
            return;
        }
        mBTGatt.close();
        mBTGatt = null;
    }

}

