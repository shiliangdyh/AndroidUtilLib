package com.github.sl;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.sl.scanlib.widget.CameraPreview;
import com.github.sl.util.LogUtils;


public class QRCodeActivity extends AppCompatActivity {
    private static final String TAG = "QRCodeActivity";

    private CameraPreview cameraPreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        LogUtils.init(this);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        cameraPreview = ((CameraPreview) findViewById(R.id.camera_preview));

    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.d(TAG, "onResume: ");
        cameraPreview.startScan();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.d(TAG, "onStop: ");
        cameraPreview.stopScan();
    }
}
