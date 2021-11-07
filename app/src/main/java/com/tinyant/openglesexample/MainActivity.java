package com.tinyant.openglesexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import javax.security.auth.login.LoginException;

public class MainActivity extends Activity {

    String TAG = "MainActivity";
    private static final int PERMISSION_CODE = 100;

    private GLSurfaceView gLSurfaceView;

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Need camera permission to run app");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            }, PERMISSION_CODE);
        } else {
            Log.i(TAG, "Has Camera permission just run app");
            setupViews();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            Log.i(TAG, "right code");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "User grant permission");
                setupViews();
            } else {
                Log.i(TAG, "User refuse permission");
            }
        }
    }

    private boolean checkCameraHardWare(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate activity...");

        requestPermission();
    }

    private void setupViews() {
        Log.i(TAG, "setupViews()");
        gLSurfaceView = new MyGLSurfaceView(this);
        setContentView(gLSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }
}