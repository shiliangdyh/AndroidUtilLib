package com.github.sl.scanlib.widget;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.github.sl.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

import sourceforge.base.base.CameraUtils;
import sourceforge.base.base.DisplayUtils;
import sourceforge.base.base.IViewFinder;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraPreview";
    private Handler autoFocusHandler = new Handler();
    private Camera camera;
    private float aspectTolerance = 0.1f;//允许的实际宽高比和理想宽高比之间的最大差值
    private boolean previewing;
    private boolean surfaceCreated;
    private boolean shouldAdjustFocusArea;
    private ArrayList<Camera.Area> focusAreas;
    private IViewFinder viewFinderView;

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
        surfaceCreated = true;
        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
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
        if (this.camera != null && this.previewing && this.surfaceCreated) {
            try {
                setupFocusAreas();
                this.camera.autoFocus(this.autoFocusCB);
            } catch (Exception var2) {
                var2.printStackTrace();
                this.scheduleAutoFocus();
            }
        }
    }

    /**
     * 设置对焦区域
     */
    private void setupFocusAreas() {
        if (!shouldAdjustFocusArea) return;

        if (camera == null) return;

        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getMaxNumFocusAreas() <= 0) {
            Log.e(TAG, "不支持设置对焦区域");
            return;
        }

        if (focusAreas == null) {
            int width = 2000, height = 2000;
            if (viewFinderView == null) {
                return;
            }
            Rect framingRect = viewFinderView.getFramingRect();//获得扫码框区域
            if (framingRect == null) return;
            int viewFinderViewWidth = ((View) viewFinderView).getWidth();
            int viewFinderViewHeight = ((View) viewFinderView).getHeight();

            //1.根据ViewFinderView和2000*2000的尺寸之比，缩放对焦区域
            Rect scaledRect = new Rect(framingRect);
            scaledRect.left = scaledRect.left * width / viewFinderViewWidth;
            scaledRect.right = scaledRect.right * width / viewFinderViewWidth;
            scaledRect.top = scaledRect.top * height / viewFinderViewHeight;
            scaledRect.bottom = scaledRect.bottom * height / viewFinderViewHeight;

            //2.旋转对焦区域
            Rect rotatedRect = new Rect(scaledRect);
            int rotationCount = getRotationCount();
            if (rotationCount == 1) {//若相机图像需要顺时针旋转90度，则将扫码框逆时针旋转90度
                rotatedRect.left = scaledRect.top;
                rotatedRect.top = 2000 - scaledRect.right;
                rotatedRect.right = scaledRect.bottom;
                rotatedRect.bottom = 2000 - scaledRect.left;
            } else if (rotationCount == 2) {//若相机图像需要顺时针旋转180度,则将扫码框逆时针旋转180度
                rotatedRect.left = 2000 - scaledRect.right;
                rotatedRect.top = 2000 - scaledRect.bottom;
                rotatedRect.right = 2000 - scaledRect.left;
                rotatedRect.bottom = 2000 - scaledRect.top;
            } else if (rotationCount == 3) {//若相机图像需要顺时针旋转270度，则将扫码框逆时针旋转270度
                rotatedRect.left = 2000 - scaledRect.bottom;
                rotatedRect.top = scaledRect.left;
                rotatedRect.right = 2000 - scaledRect.top;
                rotatedRect.bottom = scaledRect.right;
            }

            //3.坐标系平移
            Rect rect = new Rect(rotatedRect.left - 1000, rotatedRect.top - 1000, rotatedRect.right - 1000, rotatedRect.bottom - 1000);

            Camera.Area area = new Camera.Area(rect, 1000);
            focusAreas = new ArrayList<>();
            focusAreas.add(area);
        }

        parameters.setFocusAreas(focusAreas);
        camera.setParameters(parameters);
    }

    /**
     * 获取（旋转角度/90）
     */
    private int getRotationCount() {
        int displayOrientation = getDisplayOrientation();
        return displayOrientation / 90;
    }

    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        //自动对焦完成时此方法被调用
        public void onAutoFocus(boolean success, Camera camera) {
            scheduleAutoFocus();//一秒之后再次自动对焦
        }
    };

    /**
     * 一秒之后尝试自动对焦
     */
    private void scheduleAutoFocus() {
        autoFocusHandler.postDelayed(new Runnable() {
            public void run() {
                safeAutoFocus();
            }
        }, 1000);
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
