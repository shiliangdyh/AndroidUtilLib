package com.github.sl.scanlib.widget;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.github.sl.util.LogUtils;

import java.util.List;

import sourceforge.base.base.CameraUtils;
import sourceforge.base.base.DisplayUtils;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraPreview";
    private Camera camera;
    private float aspectTolerance = 0.1f;//允许的实际宽高比和理想宽高比之间的最大差值
    private boolean previewing;

    public CameraPreview(Context context) {
        super(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtils.d(TAG, "surfaceCreated: ");
        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopScan();
    }

    /**
     * 开始预览
     */
    public void startCameraPreview() {
        if (camera != null) {
            try {
                previewing = true;
                setupCameraParameters();//设置相机参数
                camera.setPreviewDisplay(getHolder());//设置在当前surfaceView中进行相机预览
                camera.setDisplayOrientation(getDisplayOrientation());//设置相机预览图像的旋转角度
                camera.setOneShotPreviewCallback(this);//设置一次性的预览回调
                camera.startPreview();//开始预览
                safeAutoFocus();//自动对焦
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 尝试自动对焦
     */
    private void safeAutoFocus() {
    }

    /**
     * 设置相机参数
     */
    private void setupCameraParameters() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size optimalSize = getOptimalPreviewSize();
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(parameters);
        }
    }

    public void startScan() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            int cameraId = CameraUtils.getDefaultCameraId();
            if (camera == null) {
                camera = CameraUtils.getCameraInstance(cameraId);//打开camera
            }
            getHolder().addCallback(this);//surface生命周期的回调
        } else {//没有相机权限
            throw new RuntimeException("没有Camera权限");
        }
    }

    public void stopScan() {
        stopCameraPreview();
    }

    /**
     * 停止预览
     */
    public synchronized void stopCameraPreview() {
        if (camera != null) {
            try {
                previewing = false;
                getHolder().removeCallback(this);
                camera.cancelAutoFocus();
                camera.setOneShotPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 找到一个合适的previewSize（根据控件的尺寸）
     */
    private Camera.Size getOptimalPreviewSize() {
        if (camera == null) {
            return null;
        }

        //相机图像默认都是横屏(即宽>高)
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        if (sizes == null) return null;
        int w, h;
        if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_LANDSCAPE) {
            w = getWidth();
            h = getHeight();
        } else {
            w = getHeight();
            h = getWidth();
        }

        double targetRatio = (double) w / h;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > aspectTolerance) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    /**
     * 要使相机图像的方向与手机中窗口的方向一致，相机图像需要顺时针旋转的角度
     * <p>
     * 此方法由google官方提供，详见Camera类中setDisplayOrientation的方法说明
     */
    public int getDisplayOrientation() {
        if (camera == null) {
            return 0;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
