package net.sourceforge.base.base;

import android.hardware.Camera;
import android.os.HandlerThread;

// This code is mostly based on the top answer here:
// http://stackoverflow.com/questions/18149964/best-use-of-handlerthread-over-other-similar-classes
public class CameraHandlerThread extends HandlerThread {
    private static final String TAG = "CameraHandlerThread";
    private BarcodeScannerView mScannerView;

    public CameraHandlerThread(BarcodeScannerView scannerView) {
        super("CameraHandlerThread");
        mScannerView = scannerView;
        start();
    }

    /**
     * 打开系统相机，并进行基本的初始化
     */
    public synchronized void startCamera(final int cameraId) {
        final Camera camera = CameraUtils.getCameraInstance(cameraId);//打开camera
        mScannerView.setupCameraPreview(CameraWrapper.getWrapper(camera, cameraId));
    }
}