package com.omri.goniometer;

import android.Manifest;
import android.content.Context;

import com.omri.goniometer.MainActivity;

import java.util.List;

import androidx.annotation.NonNull;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class PermissionsClass implements EasyPermissions.PermissionCallbacks {

    private final Context mContext;
    private final MainActivity mActivity;

    PermissionsClass(Context context, MainActivity activity) {
        this.mContext = context;
        this.mActivity = activity;
    }

    public void askPermissions(){
        String[] perms ={Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(mActivity, perms)){
            //Toast.makeText(this,"permissions granted", Toast.LENGTH_SHORT).show();
        } else {
            EasyPermissions.requestPermissions(mActivity,"אנא אשרו הרשאות מיקום", MainActivity.REQUEST_PERMISSIONS,perms);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, mContext);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(mActivity, perms)){
            new AppSettingsDialog.Builder(mActivity).build().show();
        }
    }
}
